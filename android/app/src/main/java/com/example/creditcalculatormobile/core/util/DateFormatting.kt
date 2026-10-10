package com.example.creditcalculatormobile.core.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun formatDateSafe(raw: String?): String {
    if (raw.isNullOrBlank()) return "Не указано"

    val localDateCandidates = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ISO_DATE,
    )

    localDateCandidates.forEach { formatter ->
        try {
            val parsed = LocalDate.parse(raw, formatter)
            return parsed.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        } catch (_: DateTimeParseException) {
        }
    }

    try {
        val parsed = OffsetDateTime.parse(raw)
        return parsed.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    } catch (_: DateTimeParseException) {
    }

    try {
        val parsed = LocalDateTime.parse(raw)
        return parsed.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    } catch (_: DateTimeParseException) {
    }

    return raw
}

fun formatBirthDateSafe(raw: String?): String {
    if (raw.isNullOrBlank()) return "Не указана"

    val candidates = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ISO_DATE,
    )

    candidates.forEach { formatter ->
        try {
            val parsed = LocalDate.parse(raw, formatter)
            return parsed.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        } catch (_: DateTimeParseException) {
        }
    }

    return raw
}
