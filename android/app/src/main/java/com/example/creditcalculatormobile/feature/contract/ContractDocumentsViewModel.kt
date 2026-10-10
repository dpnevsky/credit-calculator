package com.example.creditcalculatormobile.feature.contract

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.creditcalculatormobile.core.network.ApiResult
import com.example.creditcalculatormobile.data.model.ApplicationDocumentDto
import com.example.creditcalculatormobile.data.model.GetApplicationResponseDto
import com.example.creditcalculatormobile.data.model.OfferDto
import com.example.creditcalculatormobile.data.model.PaymentTypeDto
import com.example.creditcalculatormobile.data.model.RequestDocumentsRequestDto
import com.example.creditcalculatormobile.data.repository.ApplicationsRepository
import com.example.creditcalculatormobile.data.repository.DocumentsRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContractDocumentsUiState(
    val applicationId: String,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val isSigning: Boolean = false,
    val errorMessage: String? = null,
    val application: GetApplicationResponseDto? = null,
    val offers: List<OfferDto> = emptyList(),
    val documents: List<ApplicationDocumentDto> = emptyList(),
    val requestDocumentsLoading: Boolean = false,
    val selectedPaymentType: PaymentTypeDto = PaymentTypeDto.ANNUITY,
    val downloadLoadingDocumentId: String? = null,
    val actionMessage: String? = null,
) {
    val isSigned: Boolean
        get() = application?.status?.uppercase() == "SIGNED" ||
                application?.status?.uppercase() == "CONTRACT_SIGNED"
}

class ContractDocumentsViewModel(
    private val applicationId: String,
    private val applicationsRepository: ApplicationsRepository,
    private val documentsRepository: DocumentsRepository,
    private val documentDownloadManager: DocumentDownloadManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ContractDocumentsUiState(applicationId = applicationId),
    )
    val uiState: StateFlow<ContractDocumentsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private suspend fun refreshDataInternal(forceRefresh: Boolean, clearMessages: Boolean) {
        _uiState.update { current ->
            current.copy(
                loading = if (forceRefresh) false else current.application == null,
                refreshing = forceRefresh,
                errorMessage = if (clearMessages) null else current.errorMessage,
                actionMessage = if (clearMessages) null else current.actionMessage,
            )
        }

        val applicationResult = applicationsRepository.getApplication(applicationId)
        when (applicationResult) {
            is ApiResult.Failure -> {
                _uiState.update { it.copy(
                    loading = false,
                    refreshing = false,
                    errorMessage = applicationResult.error.message,
                ) }
            }

            is ApiResult.Success -> {
                val application = applicationResult.value

                val (offersRes, docsRes) = coroutineScope {
                    val offersDef = async { applicationsRepository.getOffers(applicationId) }
                    val docsDef = async { applicationsRepository.getDocuments(applicationId) }
                    offersDef.await() to docsDef.await()
                }

                val offersValue = (offersRes as? ApiResult.Success<List<OfferDto>>)?.value ?: emptyList()
                val documentsValue = (docsRes as? ApiResult.Success<List<ApplicationDocumentDto>>)?.value ?: emptyList()

                val dataError = (offersRes as? ApiResult.Failure)?.error?.message
                    ?: (docsRes as? ApiResult.Failure)?.error?.message
                _uiState.update { it.copy(
                    loading = false,
                    refreshing = false,
                    errorMessage = dataError,
                    application = application,
                    offers = offersValue,
                    documents = documentsValue,
                    selectedPaymentType = application.paymentType ?: it.selectedPaymentType,
                ) }
            }
        }
    }

    fun load(forceRefresh: Boolean = false, clearMessages: Boolean = true) {
        viewModelScope.launch {
            refreshDataInternal(forceRefresh, clearMessages)
        }
    }

    fun updateSelectedPaymentType(paymentType: PaymentTypeDto) {
        _uiState.update { it.copy(
            selectedPaymentType = paymentType,
            actionMessage = null,
        ) }
    }

    fun requestDocuments() {
        if (_uiState.value.requestDocumentsLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(
                requestDocumentsLoading = true,
                errorMessage = null,
                actionMessage = null,
            ) }

            val result = applicationsRepository.requestDocuments(
                applicationId = applicationId,
                request = RequestDocumentsRequestDto(
                    paymentType = _uiState.value.selectedPaymentType,
                ),
            )

            if (result is ApiResult.Success) {
                // Kafka/PDF generation is asynchronous: inspect the state instead of assuming a delay is enough.
                for (attempt in 1..15) {
                    refreshDataInternal(forceRefresh = true, clearMessages = false)
                    val state = _uiState.value
                    if (state.documents.isNotEmpty() || state.errorMessage != null ||
                        state.application?.status != "DOCUMENTS_REQUESTED") break
                    if (attempt < 15) delay(1000)
                }
                if (_uiState.value.documents.isEmpty() && _uiState.value.errorMessage == null) {
                    _uiState.update { it.copy(actionMessage = "Документы ещё формируются. Нажмите «Обновить» позже.") }
                }
            } else if (result is ApiResult.Failure) {
                _uiState.update { it.copy(errorMessage = result.error.message) }
            }

            // Выключаем индикатор только ПОСЛЕ загрузки новых данных
            _uiState.update { it.copy(requestDocumentsLoading = false) }
        }
    }

    fun downloadDocument(document: ApplicationDocumentDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                downloadLoadingDocumentId = document.documentId,
                errorMessage = null,
                actionMessage = null,
            ) }

            when (val downloadResult = documentsRepository.downloadDocument(document.documentId)) {
                is ApiResult.Success -> {
                    val saveResult = documentDownloadManager.saveDocument(
                        downloadResult.value.copy(
                            fileName = downloadResult.value.fileName ?: document.fileName ?: "credit-document-${document.documentId}.pdf",
                            mimeType = downloadResult.value.mimeType ?: document.mimeType,
                        ),
                    )

                    _uiState.update { current ->
                        if (saveResult.isSuccess) {
                            current.copy(
                                downloadLoadingDocumentId = null,
                                actionMessage = "Файл сохранён",
                            )
                        } else {
                            current.copy(
                                downloadLoadingDocumentId = null,
                                errorMessage = saveResult.exceptionOrNull()?.message ?: "Не удалось сохранить файл",
                            )
                        }
                    }
                }

                is ApiResult.Failure -> {
                    _uiState.update { it.copy(
                        downloadLoadingDocumentId = null,
                        errorMessage = downloadResult.error.message,
                    ) }
                }
            }
        }
    }

    fun signContract() {
        if (_uiState.value.isSigning || _uiState.value.isSigned) return

        viewModelScope.launch {
            _uiState.update { it.copy(
                isSigning = true,
                errorMessage = null,
                actionMessage = null,
            ) }

            val result = applicationsRepository.signContract(applicationId)

            if (result is ApiResult.Success) {
                _uiState.update { state -> state.copy(
                    application = state.application?.copy(status = result.value.applicationStatus),
                    actionMessage = "Учебное подтверждение подписания сохранено на сервере",
                ) }
                refreshDataInternal(forceRefresh = true, clearMessages = false)
            } else if (result is ApiResult.Failure) {
                _uiState.update { it.copy(errorMessage = result.error.message) }
            }

            _uiState.update { it.copy(isSigning = false) }
        }
    }

    companion object {
        fun factory(
            applicationsRepository: ApplicationsRepository,
            documentsRepository: DocumentsRepository,
            documentDownloadManager: DocumentDownloadManager,
            savedStateHandle: SavedStateHandle,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ContractDocumentsViewModel::class.java))
                val applicationId = checkNotNull(
                    savedStateHandle.get<String>("applicationId"),
                )
                return ContractDocumentsViewModel(
                    applicationId = applicationId,
                    applicationsRepository = applicationsRepository,
                    documentsRepository = documentsRepository,
                    documentDownloadManager = documentDownloadManager,
                ) as T
            }
        }
    }
}
