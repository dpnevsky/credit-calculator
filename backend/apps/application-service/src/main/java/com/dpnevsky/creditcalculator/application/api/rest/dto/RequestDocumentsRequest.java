package com.dpnevsky.creditcalculator.application.api.rest.dto;

import jakarta.validation.constraints.Pattern;

public record RequestDocumentsRequest(
        @Pattern(regexp = "ANNUITY|DIFFERENTIAL")
        String paymentType
) {
}
