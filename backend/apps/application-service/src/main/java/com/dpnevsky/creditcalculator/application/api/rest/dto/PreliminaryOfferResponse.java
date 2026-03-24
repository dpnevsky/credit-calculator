package com.dpnevsky.creditcalculator.application.api.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PreliminaryOfferResponse(
        UUID applicationId,
        BigDecimal requestedAmount,
        BigDecimal totalAmount,
        Integer termMonths,
        BigDecimal monthlyPayment,
        BigDecimal rate,
        boolean insuranceEnabled,
        boolean salaryClient
) {
}