package com.example.creditcalculatormobile.feature.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.creditcalculatormobile.core.ui.AppTextField
import com.example.creditcalculatormobile.core.ui.ErrorState
import com.example.creditcalculatormobile.core.ui.InfoCard
import com.example.creditcalculatormobile.core.ui.LoadingState
import com.example.creditcalculatormobile.core.ui.PrimaryButton
import com.example.creditcalculatormobile.core.ui.SecondaryButton
import com.example.creditcalculatormobile.core.ui.SectionTitle
import com.example.creditcalculatormobile.core.util.formatMoneyRub
import com.example.creditcalculatormobile.feature.calculator.model.CalculatorPaymentType
import com.example.creditcalculatormobile.feature.calculator.model.CalculatorResult
import com.example.creditcalculatormobile.feature.calculator.model.PaymentScheduleRow

@Composable
fun CalculatorRoute(
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = viewModel(factory = CalculatorViewModel.factory()),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CalculatorScreen(
        uiState = uiState,
        onAmountChange = viewModel::updateAmount,
        onMonthsChange = viewModel::updateMonths,
        onRateChange = viewModel::updateRate,
        onPaymentTypeChange = viewModel::updatePaymentType,
        onCalculateClick = viewModel::calculate,
        onClearClick = viewModel::clearInputs,
        modifier = modifier,
    )
}

@Composable
fun CalculatorScreen(
    uiState: com.example.creditcalculatormobile.feature.calculator.model.CalculatorUiState,
    onAmountChange: (String) -> Unit,
    onMonthsChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onPaymentTypeChange: (CalculatorPaymentType) -> Unit,
    onCalculateClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Убран SectionTitle, так как заголовок есть в TopBar

        item {
            InfoCard(title = "Параметры кредита") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AppTextField(
                        value = uiState.amount,
                        onValueChange = onAmountChange,
                        label = "Сумма кредита",
                        placeholder = "Например, 1000000",
                        keyboardOptions = KeyboardOptions.Default,
                    )

                    AppTextField(
                        value = uiState.months,
                        onValueChange = onMonthsChange,
                        label = "Срок, месяцев",
                        placeholder = "Например, 12",
                        keyboardOptions = KeyboardOptions.Default,
                    )

                    AppTextField(
                        value = uiState.rate,
                        onValueChange = onRateChange,
                        label = "Процентная ставка, %",
                        placeholder = "Например, 15.5",
                        keyboardOptions = KeyboardOptions.Default,
                    )

                    PaymentTypeSelector(
                        selected = uiState.paymentType,
                        onSelected = onPaymentTypeChange,
                    )

                    PaymentTypeDescription(
                        paymentType = uiState.paymentType,
                    )

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        ErrorState(message = uiState.errorMessage)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PrimaryButton(
                            text = "Рассчитать",
                            onClick = onCalculateClick,
                            modifier = Modifier.weight(1f),
                            loading = uiState.loading,
                        )
                        SecondaryButton(
                            text = "Очистить",
                            onClick = onClearClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (uiState.loading) {
            item {
                InfoCard {
                    LoadingState(message = "Выполняем расчёт кредита…")
                }
            }
        }

        uiState.result?.let { result ->
            item {
                CalculatorResultCard(result = result)
            }

            item {
                PaymentScheduleCard(rows = result.schedule)
            }
        }
    }
}

@Composable
private fun PaymentTypeSelector(
    selected: CalculatorPaymentType,
    onSelected: (CalculatorPaymentType) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Тип платежа",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalculatorPaymentType.entries.forEach { type ->
                FilterChip(
                    selected = type == selected,
                    onClick = { onSelected(type) },
                    label = {
                        Text(
                            text = when (type) {
                                CalculatorPaymentType.ANNUITY -> "Аннуитетный"
                                CalculatorPaymentType.DIFFERENTIATED -> "Дифференцированный"
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun PaymentTypeDescription(paymentType: CalculatorPaymentType) {
    InfoCard(
        title = "Описание типа платежа",
        contentPadding = PaddingValues(14.dp),
    ) {
        val description = when (paymentType) {
            CalculatorPaymentType.ANNUITY ->
                "Аннуитетный платёж сохраняет примерно одинаковую сумму ежемесячного платежа на протяжении всего срока кредита."
            CalculatorPaymentType.DIFFERENTIATED ->
                "Дифференцированный платёж постепенно уменьшается: первые платежи выше, затем сумма снижается по мере уменьшения остатка долга."
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CalculatorResultCard(result: CalculatorResult) {
    InfoCard(title = "Результаты расчёта") {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ResultRow(
                label = "Тип платежа",
                value = when (result.paymentType) {
                    CalculatorPaymentType.ANNUITY -> "Аннуитетный"
                    CalculatorPaymentType.DIFFERENTIATED -> "Дифференцированный"
                },
            )

            when (result.paymentType) {
                CalculatorPaymentType.ANNUITY -> {
                    ResultRow(
                        label = "Ежемесячный платёж",
                        value = formatMoneyRub(result.monthlyPayment ?: 0.0),
                    )
                }

                CalculatorPaymentType.DIFFERENTIATED -> {
                    ResultRow(
                        label = "Средний платёж",
                        value = formatMoneyRub(result.averageMonthlyPayment ?: 0.0),
                    )
                    ResultRow(
                        label = "Первый платёж",
                        value = formatMoneyRub(result.firstPayment ?: 0.0),
                    )
                    ResultRow(
                        label = "Последний платёж",
                        value = formatMoneyRub(result.lastPayment ?: 0.0),
                    )
                }
            }

            ResultRow(
                label = "Общая сумма выплат",
                value = formatMoneyRub(result.totalPayment),
            )
            ResultRow(
                label = "Переплата",
                value = formatMoneyRub(result.overpayment),
            )
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PaymentScheduleCard(rows: List<PaymentScheduleRow>) {
    InfoCard(title = "График платежей") {
        if (rows.isEmpty()) {
            Text(
                text = "График пока пуст.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val horizontalScroll = rememberScrollState()

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Таблица прокручивается по горизонтали. Данные рассчитаны локально на устройстве.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll),
                ) {
                    ScheduleHeaderRow()
                    rows.forEach { row ->
                        ScheduleDataRow(row = row)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleHeaderRow() {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            )
            .padding(vertical = 12.dp, horizontal = 10.dp),
    ) {
        HeaderCell("Месяц", 80.dp)
        HeaderCell("Платёж", 140.dp)
        HeaderCell("Основной долг", 160.dp)
        HeaderCell("Проценты", 140.dp)
        HeaderCell("Остаток", 160.dp)
    }
}

@Composable
private fun ScheduleDataRow(row: PaymentScheduleRow) {
    Column {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
        ) {
            DataCell(row.monthNumber.toString(), 80.dp)
            DataCell(formatMoneyRub(row.payment), 140.dp)
            DataCell(formatMoneyRub(row.principal), 160.dp)
            DataCell(formatMoneyRub(row.interest), 140.dp)
            DataCell(formatMoneyRub(row.remainingBalance), 160.dp)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier.width(width),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DataCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier.width(width),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
