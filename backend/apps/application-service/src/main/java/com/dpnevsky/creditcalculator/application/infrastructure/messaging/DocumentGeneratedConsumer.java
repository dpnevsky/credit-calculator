package com.dpnevsky.creditcalculator.application.infrastructure.messaging;

import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationDocumentEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationDocumentRepository;
import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerated;
import com.dpnevsky.creditcalculator.contracts.eventenvelope.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class DocumentGeneratedConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentGeneratedConsumer.class);

    private final ObjectMapper objectMapper;
    private final ApplicationDocumentRepository applicationDocumentRepository;

    public DocumentGeneratedConsumer(
            ObjectMapper objectMapper,
            ApplicationDocumentRepository applicationDocumentRepository
    ) {
        this.objectMapper = objectMapper;
        this.applicationDocumentRepository = applicationDocumentRepository;
    }

    @Transactional
    @KafkaListener(
            topics = "document-generated",
            groupId = "application-service"
    )
    public void consume(String rawMessage) {
        EventEnvelope<DocumentGenerated> envelope = parse(rawMessage);
        DocumentGenerated payload = envelope.payload();

        if (applicationDocumentRepository.findByDocumentId(payload.documentId()).isPresent()) {
            log.info(
                    "Skip duplicate DocumentGenerated applicationId={}, documentId={}, requestId={}",
                    payload.applicationId(),
                    payload.documentId(),
                    payload.requestId()
            );
            return;
        }

        ApplicationDocumentEntity documentEntity = new ApplicationDocumentEntity(
                UUID.randomUUID(),
                payload.applicationId(),
                payload.requestId(),
                payload.documentId(),
                payload.documentType(),
                payload.format(),
                payload.fileName(),
                payload.mimeType(),
                payload.storageKey(),
                payload.status(),
                payload.generatedAt(),
                OffsetDateTime.now()
        );

        applicationDocumentRepository.save(documentEntity);

        log.info(
                "Saved DocumentGenerated applicationId={}, documentId={}, requestId={}",
                payload.applicationId(),
                payload.documentId(),
                payload.requestId()
        );
    }

    private EventEnvelope<DocumentGenerated> parse(String rawMessage) {
        try {
            return objectMapper.readValue(
                    rawMessage,
                    new TypeReference<EventEnvelope<DocumentGenerated>>() {}
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse DocumentGenerated event", exception);
        }
    }
}