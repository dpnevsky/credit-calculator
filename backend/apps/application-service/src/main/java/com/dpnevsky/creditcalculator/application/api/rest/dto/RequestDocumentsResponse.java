package com.dpnevsky.creditcalculator.application.api.rest.dto;

import java.util.UUID;

public record RequestDocumentsResponse(
        UUID applicationId,
        String status,
        String message
) {
}