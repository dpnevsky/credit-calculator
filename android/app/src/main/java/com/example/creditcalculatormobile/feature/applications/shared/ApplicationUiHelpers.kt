package com.example.creditcalculatormobile.feature.applications.shared

import androidx.compose.ui.graphics.Color
import com.example.creditcalculatormobile.core.util.formatDateSafe
import com.example.creditcalculatormobile.core.util.formatMoneyRub
import com.example.creditcalculatormobile.core.util.formatMoneyRubPrecise
import com.example.creditcalculatormobile.data.model.GetApplicationResponseDto
import com.example.creditcalculatormobile.data.model.OfferDto
import com.example.creditcalculatormobile.data.model.PaymentTypeDto

private val rejectionReasonLabels: Map<String, String> = mapOf(
    "AGE_OUT_OF_RANGE" to "Возраст не соответствует требованиям банка.",
    "UNEMPLOYED" to "Отсутствует подтверждённая занятость.",
    "AMOUNT_EXCEEDS_24_MONTHS_OF_SALARY" to "Запрошенная сумма слишком велика относительно дохода.",
    "TOTAL_WORK_EXPERIENCE_TOO_LOW" to "Недостаточный общий трудовой стаж.",
    "CURRENT_WORK_EXPERIENCE_TOO_LOW" to "Недостаточный стаж на текущем месте работы.",
)

private data class StatusUiConfig(
    val label: String,
    val color: Color,
)

private val statusConfig: Map<String, StatusUiConfig> = mapOf(
    "DRAFT" to StatusUiConfig("Черновик", Color(0xFF9CA3AF)),
    "PRESCORING_REJECTED" to StatusUiConfig("Отказано", Color(0xFFDC2626)),
    "SUBMITTED" to StatusUiConfig("Отправлена", Color(0xFF0EA5E9)),
    "SCORING_COMPLETED" to StatusUiConfig("Предварительно одобрено", Color(0xFF9CA3AF)),
    "SCORING_APPROVED" to StatusUiConfig("Одобрено", Color(0xFFF59E0B)), // Оранжевый
    "SCORING_REJECTED" to StatusUiConfig("Отказано", Color(0xFFDC2626)),
    "OFFER_SELECTED" to StatusUiConfig("Одобрено", Color(0xFFF59E0B)),  // Оранжевый
    "DOCUMENTS_REQUESTED" to StatusUiConfig("Одобрено", Color(0xFFCA8A04)),
    "DOCUMENTS_READY" to StatusUiConfig("Одобрено", Color(0xFFCA8A04)),
    "SIGNED" to StatusUiConfig("Подписано", Color(0xFF16A34A)),
    "CONTRACT_SIGNED" to StatusUiConfig("Подписано", Color(0xFF16A34A)),
)

fun formatApplicationNumber(applicationId: String): String = applicationId.take(8)

fun normalizeApplicationStatus(status: String): String {
    val normalized = status.uppercase()
    return when (normalized) {
        "PRESCORING_FAILED" -> "PRESCORING_REJECTED"
        else -> normalized
    }
}

fun getApplicationStatusLabel(status: String): String {
    val normalized = normalizeApplicationStatus(status)
    return statusConfig[normalized]?.label ?: normalized
}

fun getApplicationStatusColor(status: String): Color {
    val normalized = normalizeApplicationStatus(status)
    return statusConfig[normalized]?.color ?: Color(0xFF6B7280)
}

fun isRejectedApplicationStatus(status: String): Boolean {
    val normalized = normalizeApplicationStatus(status)
    return normalized == "PRESCORING_REJECTED" || normalized == "SCORING_REJECTED"
}

fun getApplicationRejectionReasons(
    status: String,
    reasons: List<String> = emptyList(),
): List<String> {
    val normalized = normalizeApplicationStatus(status)

    if (reasons.isNotEmpty()) {
        return reasons.map(::formatRejectionReason)
    }

    return when (normalized) {
        "PRESCORING_REJECTED" -> listOf("Заявка не прошла предварительную проверку банка.")
        "SCORING_REJECTED" -> listOf("Банк отклонил заявку по результатам скоринга.")
        else -> emptyList()
    }
}

private fun formatRejectionReason(reason: String): String {
    val mapped = rejectionReasonLabels[reason]
    if (mapped != null) return mapped

    val humanized = reason
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.uppercase() }

    return "$humanized."
}

fun getPaymentTypeLabel(paymentType: PaymentTypeDto?): String {
    return when (paymentType) {
        PaymentTypeDto.ANNUITY -> "Аннуитетный"
        PaymentTypeDto.DIFFERENTIAL -> "Дифференцированный"
        null -> "Не указано"
    }
}

fun getSelectedOffer(offers: List<OfferDto>): OfferDto? =
    offers.firstOrNull { it.selected }

data class ContractTerms(
    val amount: Double,
    val termMonths: Int,
    val rate: Double?,
)

fun resolveContractTerms(
    application: GetApplicationResponseDto,
    selectedOffer: OfferDto?,
): ContractTerms {
    return ContractTerms(
        amount = selectedOffer?.totalAmount ?: application.amount,
        termMonths = selectedOffer?.termMonths ?: application.termMonths,
        rate = selectedOffer?.rate,
    )
}
