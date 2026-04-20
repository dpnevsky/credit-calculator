package com.dpnevsky.creditcalculator.document.api.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GetGeneratedDocumentResponse(
        UUID documentId,
        String documentType,
        String format,
        String fileName,
        String mimeType,
        String status,
        OffsetDateTime generatedAt
) {
}
