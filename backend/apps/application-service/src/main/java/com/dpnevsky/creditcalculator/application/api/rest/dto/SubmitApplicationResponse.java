package com.dpnevsky.creditcalculator.application.api.rest.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SubmitApplicationResponse(
        UUID applicationId,
        String status,
        String decision,
        BigDecimal scoreValue,
        String riskGrade,
        String rulesVersion,
        BigDecimal approvedAmount,
        Integer approvedTermMonths,
        BigDecimal approvedRate,
        List<String> reasons
) {
}