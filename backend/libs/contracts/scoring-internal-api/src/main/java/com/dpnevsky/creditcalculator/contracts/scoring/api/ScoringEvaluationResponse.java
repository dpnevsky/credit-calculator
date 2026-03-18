package com.dpnevsky.creditcalculator.contracts.scoring.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ScoringEvaluationResponse(
        @NotNull
        UUID requestId,

        @NotNull
        UUID applicationId,

        @NotBlank
        String decision,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal scoreValue,

        @NotBlank
        String riskGrade,

        @NotBlank
        String rulesVersion,

        @DecimalMin("0.00")
        BigDecimal maxApprovedAmount,

        Integer maxTermMonths,

        @DecimalMin("0.00")
        BigDecimal baseInterestRate,

        @NotNull
        List<String> reasons,

        @NotNull
        OffsetDateTime calculatedAt
) {
}