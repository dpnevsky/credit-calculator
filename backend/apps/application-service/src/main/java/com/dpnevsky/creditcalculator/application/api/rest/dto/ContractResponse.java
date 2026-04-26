package com.dpnevsky.creditcalculator.application.api.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContractResponse(
        UUID applicationId,
        String contractNumber,
        String contractStatus,
        String applicationStatus,
        boolean signed,
        OffsetDateTime signedAt,
        UUID signatureId
) {
}
