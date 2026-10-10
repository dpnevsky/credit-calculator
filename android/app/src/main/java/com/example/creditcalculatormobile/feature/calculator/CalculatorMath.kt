package com.example.creditcalculatormobile.feature.calculator

import com.example.creditcalculatormobile.feature.calculator.model.CalculatorPaymentType
import com.example.creditcalculatormobile.feature.calculator.model.CalculatorResult
import com.example.creditcalculatormobile.feature.calculator.model.PaymentScheduleRow
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.expm1
import kotlin.math.ln1p
import kotlin.math.min

internal data class ParsedCalculatorInput(
    val amount: Double,
    val months: Int,
    val rate: Double,
    val paymentType: CalculatorPaymentType,
)

internal fun parseCalculatorInput(
    amountText: String,
    monthsText: String,
    rateText: String,
    paymentType: CalculatorPaymentType,
): Result<ParsedCalculatorInput> {
    val amount = amountText.replace(',', '.').trim().toDoubleOrNull()
        ?: return Result.failure(IllegalArgumentException("Введите корректную сумму кредита"))

    val months = monthsText.trim().toIntOrNull()
        ?: return Result.failure(IllegalArgumentException("Введите корректный срок кредита"))

    val rate = rateText.replace(',', '.').trim().toDoubleOrNull()
        ?: return Result.failure(IllegalArgumentException("Введите корректную процентную ставку"))

    if (!amount.isFinite() || amount < 0.0 || amount > 1_000_000_000_000.0) {
        return Result.failure(IllegalArgumentException("Сумма кредита должна быть от 0 до 1 000 000 000 000 ₽"))
    }
    if (months !in 1..360) {
        return Result.failure(IllegalArgumentException("Срок кредита должен быть от 1 до 360 месяцев"))
    }
    if (!rate.isFinite() || rate < 0.0 || rate > 1000.0) {
        return Result.failure(IllegalArgumentException("Ставка должна быть от 0 до 1000%"))
    }

    return Result.success(
        ParsedCalculatorInput(
            amount = amount,
            months = months,
            rate = rate,
            paymentType = paymentType,
        ),
    )
}

internal fun calculateLoan(input: ParsedCalculatorInput): CalculatorResult {
    return when (input.paymentType) {
        CalculatorPaymentType.ANNUITY -> calculateAnnuity(input)
        CalculatorPaymentType.DIFFERENTIATED -> calculateDifferentiated(input)
    }
}

private fun calculateAnnuity(input: ParsedCalculatorInput): CalculatorResult {
    val monthlyRate = input.rate / 100.0 / 12.0
    val monthlyPayment = if (monthlyRate == 0.0) {
        input.amount / input.months
    } else {
        input.amount * monthlyRate / -expm1(-input.months * ln1p(monthlyRate))
    }

    val schedule = buildAnnuitySchedule(
        amount = input.amount,
        months = input.months,
        monthlyRate = monthlyRate,
        monthlyPayment = monthlyPayment,
    )

    val totalPayment = schedule.sumOf { it.payment }
    val overpayment = totalPayment - input.amount

    return CalculatorResult(
        paymentType = CalculatorPaymentType.ANNUITY,
        monthlyPayment = monthlyPayment.roundMoney(),
        averageMonthlyPayment = monthlyPayment.roundMoney(),
        firstPayment = schedule.first().payment,
        lastPayment = schedule.last().payment,
        totalPayment = totalPayment.roundMoney(),
        overpayment = overpayment.roundMoney(),
        schedule = schedule,
    )
}

private fun buildAnnuitySchedule(
    amount: Double,
    months: Int,
    monthlyRate: Double,
    monthlyPayment: Double,
): List<PaymentScheduleRow> {
    val rows = mutableListOf<PaymentScheduleRow>()
    var remaining = amount.roundMoney()

    repeat(months) { index ->
        val month = index + 1
        val interest = (remaining * monthlyRate).roundMoney()
        val isLastMonth = month == months

        val principal = if (isLastMonth) {
            remaining
        } else {
            min(remaining, max(0.0, monthlyPayment.roundMoney() - interest).roundMoney())
        }

        val payment = if (isLastMonth) {
            principal + interest
        } else {
            principal + interest
        }

        remaining = max(0.0, remaining - principal).roundMoney()

        rows += PaymentScheduleRow(
            monthNumber = month,
            payment = payment.roundMoney(),
            principal = principal.roundMoney(),
            interest = interest.roundMoney(),
            remainingBalance = remaining.roundMoney(),
        )
    }

    return rows
}

private fun calculateDifferentiated(input: ParsedCalculatorInput): CalculatorResult {
    val monthlyRate = input.rate / 100.0 / 12.0
    val principalPart = (input.amount / input.months).roundMoney()
    val schedule = mutableListOf<PaymentScheduleRow>()

    var remaining = input.amount.roundMoney()

    repeat(input.months) { index ->
        val month = index + 1
        val interest = (remaining * monthlyRate).roundMoney()
        val principal = if (month == input.months) remaining else min(remaining, principalPart)
        val payment = principal + interest
        remaining = max(0.0, remaining - principal).roundMoney()

        schedule += PaymentScheduleRow(
            monthNumber = month,
            payment = payment.roundMoney(),
            principal = principal.roundMoney(),
            interest = interest.roundMoney(),
            remainingBalance = remaining.roundMoney(),
        )
    }

    val totalPayment = schedule.sumOf { it.payment }
    val overpayment = totalPayment - input.amount
    val averageMonthlyPayment = totalPayment / input.months

    return CalculatorResult(
        paymentType = CalculatorPaymentType.DIFFERENTIATED,
        monthlyPayment = null,
        averageMonthlyPayment = averageMonthlyPayment.roundMoney(),
        firstPayment = schedule.firstOrNull()?.payment,
        lastPayment = schedule.lastOrNull()?.payment,
        totalPayment = totalPayment.roundMoney(),
        overpayment = overpayment.roundMoney(),
        schedule = schedule,
    )
}

internal fun formatMoneyRub(value: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("ru", "RU"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2
    return formatter.format(value)
}

private fun Double.roundMoney(): Double {
    return BigDecimal.valueOf(this)
        .setScale(2, RoundingMode.HALF_UP)
        .toDouble()
}
