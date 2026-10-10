package com.example.creditcalculatormobile.feature.applications.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creditcalculatormobile.AppContainer
import com.example.creditcalculatormobile.core.ui.*
import com.example.creditcalculatormobile.core.util.formatDateSafe
import com.example.creditcalculatormobile.core.util.formatMoneyRub
import com.example.creditcalculatormobile.data.model.*
import com.example.creditcalculatormobile.feature.applications.shared.formatApplicationNumber
import com.example.creditcalculatormobile.feature.applications.shared.getApplicationStatusColor
import com.example.creditcalculatormobile.feature.applications.shared.getApplicationStatusLabel
import com.example.creditcalculatormobile.feature.applications.shared.getPaymentTypeLabel
import com.example.creditcalculatormobile.feature.calculator.ParsedCalculatorInput
import com.example.creditcalculatormobile.feature.calculator.calculateLoan
import com.example.creditcalculatormobile.feature.calculator.model.CalculatorPaymentType
import com.example.creditcalculatormobile.feature.calculator.model.PaymentScheduleRow
import androidx.compose.foundation.rememberScrollState

@Composable
fun ApplicationDetailsRoute(
    appContainer: AppContainer,
    applicationId: String,
    onOpenContract: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ApplicationDetailsViewModel = viewModel(
        factory = ApplicationDetailsViewModel.factory(
            applicationsRepository = appContainer.applicationsRepository,
            savedStateHandle = SavedStateHandle(mapOf("applicationId" to applicationId)),
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ApplicationDetailsScreen(
        uiState = uiState,
        onRetry = { viewModel.load(forceRefresh = true) },
        onSelectOffer = viewModel::selectOffer,
        onOpenContract = onOpenContract,
        onAccountNumberChange = viewModel::updateSalaryAccountNumber,
        onPaymentTypeChange = viewModel::updatePaymentType,
        onPreviewOffer = viewModel::setPreviewOffer,
        modifier = modifier,
    )
}

@Composable
fun ApplicationDetailsScreen(
    uiState: ApplicationDetailsUiState,
    onRetry: () -> Unit,
    onSelectOffer: (String) -> Unit,
    onOpenContract: (String) -> Unit,
    onAccountNumberChange: (String) -> Unit,
    onPaymentTypeChange: (PaymentTypeDto) -> Unit,
    onPreviewOffer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionTitle(
                title = "Заявка #${formatApplicationNumber(uiState.applicationId)}",
            )
            if (uiState.refreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Блок уведомлений об ошибках
        val error = uiState.errorMessage
        if (!error.isNullOrBlank()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                    Text(text = error, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        when {
            uiState.loading && uiState.application == null -> {
                item { LoadingState(message = "Загрузка данных...") }
            }

            uiState.application == null -> {
                item {
                    ErrorState(message = "Заявка не найдена")
                    PrimaryButton(text = "Повторить", onClick = onRetry)
                }
            }

            else -> {
                val application = uiState.application!!
                val isRejected = application.status.contains("REJECTED", ignoreCase = true)
                val selectedOnBackend = uiState.offers.find { it.selected }
                val anyOfferSelected = selectedOnBackend != null

                // Предложение для просмотра: либо выбранное на бэкенде, либо то, на которое нажал пользователь
                val currentPreviewOffer = uiState.offers.find { it.offerId == uiState.previewOfferId }

                item {
                    ApplicationSummaryCard(application)
                }

                // ПОЛНАЯ ИНФОРМАЦИЯ ИЗ АНКЕТЫ
                if (application.submitData != null) {
                    item {
                        ApplicationFullDetailsCard(application)
                    }
                }

                // ВЫБОР ТИПА ПЛАТЕЖА (ВСЕГДА ДОСТУПЕН ДЛЯ ПРЕДПРОСМОТРА, ЕСЛИ ОФФЕР НЕ ВЫБРАН)
                if (!isRejected && !anyOfferSelected) {
                    item {
                        InfoCard(title = "Настройка платежей") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Выберите способ погашения для обновления графика", style = MaterialTheme.typography.labelLarge)
                                PaymentTypeSelector(
                                    selected = uiState.selectedPaymentType,
                                    onSelected = onPaymentTypeChange
                                )
                            }
                        }
                    }
                }

                if (isRejected) {
                    item {
                        InfoCard {
                            ErrorState(message = "К сожалению, ваша заявка отклонена банком.")
                        }
                    }
                } else if (uiState.offers.isNotEmpty()) {
                    item {
                        Text(
                            text = if (anyOfferSelected) "Выбранное предложение" else "Доступные предложения (нажмите для просмотра графика)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    uiState.offers.forEachIndexed { index, offer ->
                        if (!anyOfferSelected || offer.selected) {
                            item {
                                OfferCard(
                                    offer = offer,
                                    index = index + 1,
                                    isEnabled = !anyOfferSelected,
                                    isPreviewed = offer.offerId == uiState.previewOfferId,
                                    isLoading = uiState.selectOfferLoadingId == offer.offerId,
                                    accountNumber = uiState.salaryAccountNumber,
                                    accountNumberError = if (uiState.selectOfferLoadingId == offer.offerId) uiState.salaryAccountNumberError else null,
                                    onAccountNumberChange = onAccountNumberChange,
                                    onSelect = { onSelectOffer(offer.offerId) },
                                    onClick = { onPreviewOffer(offer.offerId) }
                                )
                            }
                        }
                    }
                }

                // ГРАФИК ПЛАТЕЖЕЙ: РЕАГИРУЕТ НА uiState.selectedPaymentType
                if (currentPreviewOffer != null) {
                    item {
                        // Для графика используем текущий выбор пользователя на экране,
                        // чтобы он мог видеть изменения ПРИ ПЕРЕКЛЮЧЕНИИ
                        val calcPaymentType = if (uiState.selectedPaymentType == PaymentTypeDto.ANNUITY) {
                            CalculatorPaymentType.ANNUITY
                        } else {
                            CalculatorPaymentType.DIFFERENTIATED
                        }

                        val schedule = calculateLoan(
                            ParsedCalculatorInput(
                                amount = currentPreviewOffer.totalAmount,
                                months = currentPreviewOffer.termMonths,
                                rate = currentPreviewOffer.rate,
                                paymentType = calcPaymentType
                            )
                        ).schedule

                        PaymentScheduleCard(
                            title = "График платежей (${if (calcPaymentType == CalculatorPaymentType.ANNUITY) "Аннуитетный" else "Дифф."})",
                            rows = schedule
                        )
                    }
                }

                if (anyOfferSelected) {
                    item {
                        PrimaryButton(
                            text = "Оформить договор",
                            onClick = { onOpenContract(application.applicationId) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationSummaryCard(application: GetApplicationResponseDto) {
    InfoCard(title = "Общая информация") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Badge(
                containerColor = getApplicationStatusColor(application.status),
                contentColor = Color.White
            ) {
                Text(
                    text = getApplicationStatusLabel(application.status),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            DetailRow("Сумма", formatMoneyRub(application.amount))
            DetailRow("Срок", "${application.termMonths} месяцев")
            DetailRow("Создана", formatDateSafe(application.createdAt))
            if (application.paymentType != null) {
                DetailRow("Способ погашения", getPaymentTypeLabel(application.paymentType))
            }
        }
    }
}

@Composable
private fun ApplicationFullDetailsCard(application: GetApplicationResponseDto) {
    val data = application.submitData ?: return
    val employment = data.employment

    InfoCard(title = "Данные анкеты") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Персональные данные", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                DetailRow("ФИО", "${application.lastName} ${application.firstName} ${application.middleName ?: ""}")
                DetailRow("Дата рождения", formatDateSafe(application.birthDate))
                DetailRow("Пол", when(data.gender) {
                    GenderDto.MALE -> "Мужской"
                    GenderDto.FEMALE -> "Женский"
                    GenderDto.NON_BINARY -> "Иной"
                    null -> "Не указан"
                })
                DetailRow("Семейное положение", when(data.maritalStatus) {
                    MaritalStatusDto.SINGLE -> "Холост / Не замужем"
                    MaritalStatusDto.MARRIED -> "Женат / Замужем"
                    MaritalStatusDto.DIVORCED -> "Разведен / Разведена"
                    MaritalStatusDto.WIDOWED -> "Вдовец / Вдова"
                    null -> "Не указано"
                })
                DetailRow("Иждивенцы", data.dependentAmount?.toString() ?: "0")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Паспортные данные", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                DetailRow("Серия и номер", "${application.passportSeries} ${application.passportNumber}")
                DetailRow("Дата выдачи", formatDateSafe(data.passportIssueDate))
                DetailRow("Код подразделения", data.passportIssueBranch ?: "Не указан")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Занятость и доход", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                DetailRow("Статус", when(employment?.employmentStatus) {
                    EmploymentStatusDto.EMPLOYED -> "Работает"
                    EmploymentStatusDto.UNEMPLOYED -> "Безработный"
                    EmploymentStatusDto.RETIRED -> "Пенсионер"
                    EmploymentStatusDto.SELF_EMPLOYED -> "Самозанятый"
                    EmploymentStatusDto.BUSINESS_OWNER -> "Владелец бизнеса"
                    EmploymentStatusDto.STUDENT -> "Студент"
                    null -> "Не указано"
                })
                DetailRow("Должность", when(employment?.position) {
                    PositionDto.MID_MANAGER -> "Менеджер среднего звена"
                    PositionDto.TOP_MANAGER -> "Топ-менеджер"
                    PositionDto.JUNIOR_MANAGER -> "Младший менеджер"
                    PositionDto.DEVELOPER -> "Разработчик"
                    PositionDto.SALES -> "Продажи"
                    PositionDto.ACCOUNTANT -> "Бухгалтер"
                    PositionDto.HR -> "HR-специалист"
                    PositionDto.OTHER -> "Другое"
                    null -> "Не указано"
                })
                DetailRow("ИНН работодателя", employment?.employerInn ?: "Не указан")
                DetailRow("Месячный доход", formatMoneyRub(employment?.salary ?: 0.0))
                DetailRow("Общий стаж", "${employment?.workExperienceTotal ?: 0} мес.")
                DetailRow("Стаж на тек. месте", "${employment?.workExperienceCurrent ?: 0} мес.")
            }

            if (!data.accountNumber.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                DetailRow("Номер счета", data.accountNumber!!)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentTypeSelector(
    selected: PaymentTypeDto,
    onSelected: (PaymentTypeDto) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PaymentTypeDto.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text(getPaymentTypeLabel(type)) }
            )
        }
    }
}

@Composable
private fun OfferCard(
    offer: OfferDto,
    index: Int,
    isEnabled: Boolean,
    isPreviewed: Boolean,
    isLoading: Boolean,
    accountNumber: String,
    accountNumberError: String?,
    onAccountNumberChange: (String) -> Unit,
    onSelect: () -> Unit,
    onClick: () -> Unit
) {
    val successGreen = Color(0xFF16A34A)
    val previewBlue = MaterialTheme.colorScheme.primary

    val borderColor = when {
        offer.selected -> successGreen
        isPreviewed && isEnabled -> previewBlue
        else -> Color.Transparent
    }

    val cardColor = when {
        offer.selected -> successGreen.copy(alpha = 0.05f)
        isPreviewed && isEnabled -> previewBlue.copy(alpha = 0.05f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Предложение $index", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (offer.selected) {
                    Badge(containerColor = successGreen) {
                        Text("ВЫБРАНО", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                } else if (isPreviewed && isEnabled) {
                    Badge(containerColor = previewBlue) {
                        Text("ПРОСМОТР", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            DetailRow("Сумма кредита", formatMoneyRub(offer.totalAmount))
            DetailRow("Срок", "${offer.termMonths} месяцев")
            DetailRow("Ставка", "${offer.rate}%")
            DetailRow("Ежемесячный платёж", formatMoneyRub(offer.monthlyPayment))

            val insurancePrice = if (offer.insuranceEnabled) {
                val price = offer.totalAmount - offer.requestedAmount
                if (price > 0) formatMoneyRub(price) else "Включено"
            } else {
                "Не подключено"
            }
            DetailRow("Страхование", insurancePrice)
            DetailRow("Зарплатный клиент", if (offer.salaryClient) "Да" else "Нет")

            if (!offer.selected && isEnabled) {
                if (offer.salaryClient) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = accountNumber,
                        onValueChange = onAccountNumberChange,
                        label = "Банковский счёт зарплатного клиента",
                        placeholder = "20 цифр",
                        error = accountNumberError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                PrimaryButton(
                    text = "Выбрать предложение",
                    onClick = onSelect,
                    loading = isLoading,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PaymentScheduleCard(title: String, rows: List<PaymentScheduleRow>) {
    InfoCard(title = title) {
        val horizontalScroll = rememberScrollState()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Детализация ежемесячных выплат.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScroll),
            ) {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    TableCell("№", 40.dp)
                    TableCell("Платёж", 100.dp)
                    TableCell("Осн. долг", 100.dp)
                    TableCell("Проценты", 100.dp)
                    TableCell("Остаток", 120.dp)
                }
                rows.forEach { row ->
                    Row(modifier = Modifier.padding(8.dp)) {
                        TableCell(row.monthNumber.toString(), 40.dp)
                        TableCell(formatMoneyRub(row.payment), 100.dp)
                        TableCell(row.principal.toString().let { formatMoneyRub(row.principal) }, 100.dp)
                        TableCell(row.interest.toString().let { formatMoneyRub(row.interest) }, 100.dp)
                        TableCell(formatMoneyRub(row.remainingBalance), 120.dp)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
private fun TableCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
