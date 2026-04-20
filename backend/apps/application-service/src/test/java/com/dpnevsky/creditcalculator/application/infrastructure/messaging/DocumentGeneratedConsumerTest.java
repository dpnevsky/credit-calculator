package com.dpnevsky.creditcalculator.application.infrastructure.messaging;

import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationDocumentEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationDocumentRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerated;
import com.dpnevsky.creditcalculator.contracts.eventenvelope.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentGeneratedConsumerTest {

    @Test
    void healsApplicationStatusForDuplicateDocumentEvents() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ApplicationDocumentRepository applicationDocumentRepository = mock(ApplicationDocumentRepository.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        DocumentGeneratedConsumer consumer = new DocumentGeneratedConsumer(
                objectMapper,
                applicationDocumentRepository,
                applicationRepository
        );

        UUID applicationId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        DocumentGenerated payload = new DocumentGenerated(
                UUID.randomUUID(),
                applicationId,
                documentId,
                "CREDIT_AGREEMENT",
                "PDF",
                "credit-agreement.pdf",
                "application/pdf",
                "documents/credit-agreement.pdf",
                "GENERATED",
                OffsetDateTime.of(2026, 4, 17, 13, 0, 0, 0, ZoneOffset.UTC)
        );
        EventEnvelope<DocumentGenerated> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                "DocumentGenerated",
                1,
                OffsetDateTime.of(2026, 4, 17, 13, 0, 0, 0, ZoneOffset.UTC),
                "document-service",
                applicationId.toString(),
                null,
                payload
        );

        when(applicationDocumentRepository.findByDocumentId(documentId))
                .thenReturn(Optional.of(buildDocument(applicationId, documentId)));
        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.of(buildApplication(applicationId, "DOCUMENTS_REQUESTED")));

        consumer.consume(objectMapper.writeValueAsString(envelope));

        verify(applicationDocumentRepository, never()).save(any(ApplicationDocumentEntity.class));
        verify(applicationRepository).save(argThat(application ->
                applicationId.equals(application.getId())
                        && "DOCUMENTS_READY".equals(application.getStatus())
        ));
    }

    private ApplicationDocumentEntity buildDocument(UUID applicationId, UUID documentId) {
        OffsetDateTime now = OffsetDateTime.of(2026, 4, 17, 13, 0, 0, 0, ZoneOffset.UTC);
        return new ApplicationDocumentEntity(
                UUID.randomUUID(),
                applicationId,
                UUID.randomUUID(),
                documentId,
                "CREDIT_AGREEMENT",
                "PDF",
                "credit-agreement.pdf",
                "application/pdf",
                "documents/credit-agreement.pdf",
                "GENERATED",
                now,
                now
        );
    }

    private ApplicationEntity buildApplication(UUID applicationId, String status) {
        OffsetDateTime now = OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC);
        return new ApplicationEntity(
                applicationId,
                status,
                new BigDecimal("500000.00"),
                12,
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "owner@example.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "567890",
                now,
                now,
                "ANNUITY"
        );
    }
}
