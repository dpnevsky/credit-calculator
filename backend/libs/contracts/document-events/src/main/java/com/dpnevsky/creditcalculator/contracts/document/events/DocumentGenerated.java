package com.dpnevsky.creditcalculator.contracts.document.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentGenerated(
        @NotNull
        UUID requestId,

        @NotNull
        UUID applicationId,

        @NotNull
        UUID documentId,

        @NotBlank
        String documentType,

        @NotBlank
        String format,

        @NotBlank
        String fileName,

        @NotBlank
        String mimeType,

        @NotBlank
        String storageBackend,

        @NotBlank
        String storageKey,

        @NotNull
        Long fileSizeBytes,

        @NotBlank
        String checksumSha256,

        @NotNull
        OffsetDateTime generatedAt
) {
}