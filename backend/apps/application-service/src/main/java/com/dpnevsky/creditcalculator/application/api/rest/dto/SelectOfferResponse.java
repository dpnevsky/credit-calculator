package com.dpnevsky.creditcalculator.application.api.rest.dto;

import java.util.UUID;

public record SelectOfferResponse(
        UUID applicationId,
        UUID offerId,
        String applicationStatus,
        String message
) {
}
