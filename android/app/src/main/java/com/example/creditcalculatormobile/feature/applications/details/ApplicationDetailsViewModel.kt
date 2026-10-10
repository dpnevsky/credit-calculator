package com.example.creditcalculatormobile.feature.applications.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.creditcalculatormobile.core.network.ApiResult
import com.example.creditcalculatormobile.data.model.*
import com.example.creditcalculatormobile.data.repository.ApplicationsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApplicationDetailsUiState(
    val applicationId: String,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val errorMessage: String? = null,
    val application: GetApplicationResponseDto? = null,
    val scoringResult: ScoringResultDto? = null,
    val offers: List<OfferDto> = emptyList(),
    val documents: List<ApplicationDocumentDto> = emptyList(),
    val selectOfferLoadingId: String? = null,
    val actionMessage: String? = null,
    val salaryAccountNumber: String = "",
    val salaryAccountNumberError: String? = null,
    val selectedPaymentType: PaymentTypeDto = PaymentTypeDto.ANNUITY,
    val previewOfferId: String? = null, // ID предложения для предварительного просмотра графика
)

class ApplicationDetailsViewModel(
    private val applicationId: String,
    private val applicationsRepository: ApplicationsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ApplicationDetailsUiState(applicationId = applicationId),
    )
    val uiState: StateFlow<ApplicationDetailsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load(forceRefresh: Boolean = false, clearMessages: Boolean = true) {
        viewModelScope.launch {
            val current = _uiState.value
            _uiState.value = current.copy(
                loading = if (forceRefresh) false else current.application == null,
                refreshing = forceRefresh,
                errorMessage = if (clearMessages) null else current.errorMessage,
                actionMessage = if (clearMessages) null else current.actionMessage,
            )

            val applicationResult = applicationsRepository.getApplication(applicationId)
            when (applicationResult) {
                is ApiResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        refreshing = false,
                        errorMessage = applicationResult.error.message,
                    )
                }

                is ApiResult.Success -> {
                    val application = applicationResult.value

                    val (scoringResult, offers, documents) = coroutineScope {
                        val scoringDeferred = async { applicationsRepository.getScoringResult(applicationId) }
                        val offersDeferred = async { applicationsRepository.getOffers(applicationId) }
                        val documentsDeferred = async { applicationsRepository.getDocuments(applicationId) }

                        Triple(
                            scoringDeferred.await(),
                            offersDeferred.await(),
                            documentsDeferred.await()
                        )
                    }

                    val scoringValue = (scoringResult as? ApiResult.Success<ScoringResultDto>)?.value
                    val offersValue = (offers as? ApiResult.Success<List<OfferDto>>)?.value ?: emptyList()
                    val documentsValue = (documents as? ApiResult.Success<List<ApplicationDocumentDto>>)?.value ?: emptyList()

                    val selectedOnBackend = offersValue.find { it.selected }?.offerId

                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        refreshing = false,
                        application = application,
                        scoringResult = scoringValue,
                        offers = offersValue,
                        documents = documentsValue,
                        salaryAccountNumber = application.submitData?.accountNumber ?: current.salaryAccountNumber,
                        selectedPaymentType = application.paymentType ?: current.selectedPaymentType,
                        previewOfferId = selectedOnBackend ?: current.previewOfferId ?: offersValue.firstOrNull()?.offerId
                    )
                }
            }
        }
    }

    fun setPreviewOffer(offerId: String) {
        _uiState.value = _uiState.value.copy(previewOfferId = offerId)
    }

    fun updateSalaryAccountNumber(value: String) {
        val digits = value.filter { it.isDigit() }.take(20)
        _uiState.value = _uiState.value.copy(
            salaryAccountNumber = digits,
            salaryAccountNumberError = null
        )
    }

    fun updatePaymentType(type: PaymentTypeDto) {
        _uiState.value = _uiState.value.copy(selectedPaymentType = type)
    }

    fun selectOffer(offerId: String) {
        val currentState = _uiState.value
        if (currentState.selectOfferLoadingId != null) return

        val offer = currentState.offers.find { it.offerId == offerId } ?: return

        if (offer.salaryClient) {
            if (currentState.salaryAccountNumber.length != 20) {
                _uiState.value = currentState.copy(
                    salaryAccountNumberError = "Введите ровно 20 цифр банковского счёта для зарплатного предложения."
                )
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectOfferLoadingId = offerId,
                actionMessage = null,
                errorMessage = null,
            )

            delay(300)

            val request = SelectOfferRequestDto(
                paymentType = currentState.selectedPaymentType,
                accountNumber = if (offer.salaryClient) currentState.salaryAccountNumber else null
            )

            when (val result = applicationsRepository.selectOffer(applicationId, offerId, request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        selectOfferLoadingId = null,
                        actionMessage = result.value.message.ifBlank { "Предложение успешно выбрано" },
                        salaryAccountNumberError = null,
                        previewOfferId = offerId
                    )
                    load(forceRefresh = true, clearMessages = false)
                }

                is ApiResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        selectOfferLoadingId = null,
                        errorMessage = result.error.message,
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            applicationsRepository: ApplicationsRepository,
            savedStateHandle: SavedStateHandle,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ApplicationDetailsViewModel::class.java))
                val applicationId = checkNotNull(
                    savedStateHandle.get<String>("applicationId"),
                )
                return ApplicationDetailsViewModel(
                    applicationId = applicationId,
                    applicationsRepository = applicationsRepository,
                ) as T
            }
        }
    }
}
