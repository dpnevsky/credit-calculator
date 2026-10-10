package com.example.creditcalculatormobile.feature.calculator.model

enum class CalculatorPaymentType {
    ANNUITY,
    DIFFERENTIATED,
}

data class PaymentScheduleRow(
    val monthNumber: Int,
    val payment: Double,
    val principal: Double,
    val interest: Double,
    val remainingBalance: Double,
)

data class CalculatorResult(
    val paymentType: CalculatorPaymentType,
    val monthlyPayment: Double? = null,
    val averageMonthlyPayment: Double? = null,
    val firstPayment: Double? = null,
    val lastPayment: Double? = null,
    val totalPayment: Double,
    val overpayment: Double,
    val schedule: List<PaymentScheduleRow>,
)

data class CalculatorUiState(
    val amount: String = "1000000",
    val months: String = "12",
    val rate: String = "15.5",
    val paymentType: CalculatorPaymentType = CalculatorPaymentType.ANNUITY,
    val result: CalculatorResult? = null,
    val loading: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasAnyInput: Boolean
        get() = amount.isNotBlank() || months.isNotBlank() || rate.isNotBlank()
}
