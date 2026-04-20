package com.dpnevsky.creditcalculator.contracts.document.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreditAgreementRenderData(
        @NotBlank
        String borrowerFirstName,

        @NotBlank
        String borrowerLastName,

        String borrowerMiddleName,

        @NotBlank
        String passportSeries,

        @NotBlank
        String passportNumber,

        @NotNull
        BigDecimal creditAmount,

        @NotNull
        Integer termMonths,

        @NotNull
        BigDecimal annualRate,

        @NotBlank
        String paymentType
) {
}
