package com.dpnevsky.creditcalculator.application.api.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GetApplicationDocumentResponse(
        UUID documentId,
        String documentType,
        String format,
        String fileName,
        String mimeType,
        String status,
        OffsetDateTime generatedAt
) {
}
