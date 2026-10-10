package com.example.creditcalculatormobile.core.util

import java.text.NumberFormat
import java.util.Locale

private val rubLocale = Locale("ru", "RU")

fun formatMoneyRub(value: Double): String {
    return NumberFormat.getCurrencyInstance(rubLocale).apply {
        maximumFractionDigits = 0
    }.format(value)
}

fun formatMoneyRubPrecise(value: Double): String {
    return NumberFormat.getCurrencyInstance(rubLocale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)
}
