package com.dpnevsky.creditcalculator.contracts.document.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DocumentGenerationRequested(
        @NotNull
        UUID requestId,

        @NotNull
        UUID applicationId,

        @NotBlank
        String documentType,

        @NotEmpty
        List<String> formats,

        @NotBlank
        String templateCode,

        @NotBlank
        String templateVersion,

        @NotBlank
        String requestedByUserId,

        CreditAgreementRenderData creditAgreementData,

        @NotNull
        OffsetDateTime requestedAt,

        @NotNull
        Map<String, Object> renderContext
) {
}
