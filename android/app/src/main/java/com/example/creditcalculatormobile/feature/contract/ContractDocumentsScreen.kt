package com.example.creditcalculatormobile.feature.contract

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creditcalculatormobile.AppContainer
import com.example.creditcalculatormobile.core.ui.*
import com.example.creditcalculatormobile.core.util.formatMoneyRub
import com.example.creditcalculatormobile.data.model.ApplicationDocumentDto
import com.example.creditcalculatormobile.feature.applications.shared.getSelectedOffer
import com.example.creditcalculatormobile.feature.applications.shared.resolveContractTerms
import com.example.creditcalculatormobile.feature.applications.shared.formatApplicationNumber

@Composable
fun ContractDocumentsRoute(
    appContainer: AppContainer,
    applicationId: String,
    modifier: Modifier = Modifier,
    viewModel: ContractDocumentsViewModel = viewModel(
        factory = ContractDocumentsViewModel.factory(
            applicationsRepository = appContainer.applicationsRepository,
            documentsRepository = appContainer.documentsRepository,
            documentDownloadManager = appContainer.documentDownloadManager,
            savedStateHandle = SavedStateHandle(mapOf("applicationId" to applicationId)),
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ContractDocumentsScreen(
        uiState = uiState,
        onRetry = { viewModel.load(forceRefresh = true) },
        onRequestDocuments = viewModel::requestDocuments,
        onDownloadDocument = viewModel::downloadDocument,
        onSignContract = viewModel::signContract,
        modifier = modifier,
    )
}

@Composable
fun ContractDocumentsScreen(
    uiState: ContractDocumentsUiState,
    onRetry: () -> Unit,
    onRequestDocuments: () -> Unit,
    onDownloadDocument: (ApplicationDocumentDto) -> Unit,
    onSignContract: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSignConfirmation by remember { mutableStateOf(false) }

    if (showSignConfirmation) {
        AlertDialog(
            onDismissRequest = { showSignConfirmation = false },
            title = { Text("Подтверждение подписания") },
            text = { Text("Вы уверены, что хотите подписать кредитный договор? Это действие нельзя будет отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignConfirmation = false
                    onSignContract()
                }) {
                    Text("ПОДПИСАТЬ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignConfirmation = false }) {
                    Text("ОТМЕНА")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Учебный договор: подтверждение сохраняется на сервере. Электронная подпись и выдача кредита не выполняются.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Ошибки
        val error = uiState.errorMessage
        if (!error.isNullOrBlank()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(error, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        when {
            uiState.loading && uiState.application == null -> {
                item { LoadingState(message = "Загружаем данные…") }
            }

            uiState.application == null -> {
                item {
                    ErrorState(message = "Заявка не найдена")
                    PrimaryButton(text = "Повторить", onClick = onRetry)
                }
            }

            else -> {
                val selectedOffer = getSelectedOffer(uiState.offers)
                val contractTerms = resolveContractTerms(uiState.application!!, selectedOffer)

                item {
                    InfoCard(title = "Основные условия") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryRow("Номер заявки", formatApplicationNumber(uiState.application!!.applicationId))
                            SummaryRow("Сумма договора", formatMoneyRub(contractTerms.amount))
                            SummaryRow("Срок", "${contractTerms.termMonths} мес.")
                            SummaryRow("Ставка", contractTerms.rate?.let { "$it%" } ?: "Не указана")
                            SummaryRow("Статус", if (uiState.isSigned) "ПОДПИСАНО" else "Ожидает подписи")
                        }
                    }
                }

                item {
                    InfoCard(title = "Договор") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (uiState.documents.isEmpty() && !uiState.isSigned) {
                                PrimaryButton(
                                    text = "Запросить документы",
                                    onClick = onRequestDocuments,
                                    loading = uiState.requestDocumentsLoading
                                )
                            } else if (uiState.documents.isNotEmpty()) {
                                val mainDoc = uiState.documents.first()
                                PrimaryButton(
                                    text = "Скачать договор",
                                    onClick = { onDownloadDocument(mainDoc) },
                                    loading = uiState.downloadLoadingDocumentId == mainDoc.documentId,
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                Text("Документы оформлены", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                item {
                    val signedColor = Color(0xFF16A34A)
                    val canSign = uiState.documents.isNotEmpty() && uiState.application?.status == "DOCUMENTS_READY" && !uiState.isSigned

                    if (canSign || uiState.isSigned) {
                        InfoCard(title = "Завершение оформления") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (uiState.isSigned) {
                                    Badge(containerColor = signedColor, contentColor = Color.White) {
                                        Text("ДОГОВОР ПОДПИСАН", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    PrimaryButton(
                                        text = "Подписать договор",
                                        onClick = { showSignConfirmation = true },
                                        loading = uiState.isSigning,
                                        enabled = canSign,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
