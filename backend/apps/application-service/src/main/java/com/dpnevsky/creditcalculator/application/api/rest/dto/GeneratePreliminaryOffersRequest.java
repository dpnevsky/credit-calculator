package com.dpnevsky.creditcalculator.application.api.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record GeneratePreliminaryOffersRequest(
        @NotNull
        UUID applicationId,

        @NotNull
        @DecimalMin("20000.00")
        BigDecimal requestedAmount,

        @NotNull
        @Min(6)
        Integer termMonths
) {
}