package com.dpnevsky.creditcalculator.contracts.eventenvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventEnvelope<T>(
        @NotNull
        UUID eventId,

        @NotBlank
        String eventType,

        @NotNull
        Integer eventVersion,

        @NotNull
        OffsetDateTime occurredAt,

        @NotBlank
        String producer,

        @NotBlank
        String correlationId,

        String causationId,

        @NotNull
        @Valid
        T payload
) {
}