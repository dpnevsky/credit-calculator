package com.dpnevsky.creditcalculator.document.api.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GetGeneratedDocumentResponse(
        UUID documentId,
        UUID requestId,
        UUID applicationId,
        String documentType,
        String format,
        String fileName,
        String mimeType,
        String storageKey,
        String status,
        OffsetDateTime generatedAt
) {
}