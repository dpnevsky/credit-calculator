package com.dpnevsky.creditcalculator.contracts.document.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentGenerationFailed(
        @NotNull
        UUID requestId,

        @NotNull
        UUID applicationId,

        @NotBlank
        String documentType,

        @NotBlank
        String format,

        @NotBlank
        String errorCode,

        @NotBlank
        String errorMessage,

        @NotNull
        Boolean retriable,

        @NotNull
        OffsetDateTime failedAt
) {
}