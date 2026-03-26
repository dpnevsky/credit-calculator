package com.dpnevsky.creditcalculator.application.api.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record GetApplicationScoringResultResponse(
        UUID applicationId,
        String decision,
        BigDecimal scoreValue,
        String riskGrade,
        String rulesVersion,
        BigDecimal approvedAmount,
        Integer approvedTermMonths,
        BigDecimal approvedRate,
        List<String> reasons,
        OffsetDateTime scoredAt
) {
}