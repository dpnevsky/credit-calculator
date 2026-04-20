package com.dpnevsky.creditcalculator.application.api.rest.dto;

import jakarta.validation.constraints.Pattern;

public record SelectOfferRequest(
        @Pattern(regexp = "ANNUITY|DIFFERENTIAL")
        String paymentType
) {
}
