package com.dpnevsky.creditcalculator.contracts.scoring.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScoringEvaluationRequest(
        @NotNull
        UUID requestId,

        @NotNull
        UUID applicationId,

        @NotBlank
        String productCode,

        @NotNull
        OffsetDateTime requestedAt,

        @NotNull
        @Valid
        Applicant applicant,

        @NotNull
        @Valid
        LoanRequest loanRequest,

        @NotNull
        @Valid
        PrescoringSnapshot prescoring
) {

    public record Applicant(
            @NotNull
            LocalDate birthDate,

            @NotBlank
            String employmentStatus,

            @NotNull
            @DecimalMin("0.00")
            BigDecimal monthlyIncome,

            @NotNull
            @DecimalMin("0.00")
            BigDecimal monthlyExpenses,

            @NotNull
            @DecimalMin("0.00")
            BigDecimal existingDebt
    ) {
    }

    public record LoanRequest(
            @NotNull
            @DecimalMin("0.01")
            BigDecimal amount,

            @NotNull
            @Min(1)
            Integer termMonths,

            @NotBlank
            String currency
    ) {
    }

    public record PrescoringSnapshot(
            @NotNull
            Boolean ageValid,

            @NotNull
            Boolean incomeValid,

            @NotNull
            @DecimalMin("0.00")
            BigDecimal dtiRatio,

            @NotNull
            Boolean prescorePassed
    ) {
    }
}