package com.dpnevsky.creditcalculator.application.api.rest.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateApplicationResponse(
        UUID applicationId,
        String status,
        BigDecimal amount,
        Integer termMonths,
        List<String> prescoringReasons,
        List<PreliminaryOfferResponse> preliminaryOffers
) {
}