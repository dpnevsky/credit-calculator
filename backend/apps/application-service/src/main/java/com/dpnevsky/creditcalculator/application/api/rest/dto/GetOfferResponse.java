package com.dpnevsky.creditcalculator.application.api.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GetOfferResponse(
        UUID offerId,
        UUID applicationId,
        BigDecimal requestedAmount,
        BigDecimal totalAmount,
        Integer termMonths,
        BigDecimal monthlyPayment,
        BigDecimal rate,
        Boolean insuranceEnabled,
        Boolean salaryClient,
        Boolean selected,
        OffsetDateTime createdAt
) {
}
