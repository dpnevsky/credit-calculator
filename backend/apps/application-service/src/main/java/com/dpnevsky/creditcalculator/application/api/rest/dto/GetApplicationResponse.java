package com.dpnevsky.creditcalculator.application.api.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GetApplicationResponse(
        UUID applicationId,
        String status,
        BigDecimal amount,
        Integer termMonths,
        String firstName,
        String lastName,
        String middleName,
        String email,
        LocalDate birthDate,
        String passportSeries,
        String passportNumber,
        OffsetDateTime createdAt
) {
}