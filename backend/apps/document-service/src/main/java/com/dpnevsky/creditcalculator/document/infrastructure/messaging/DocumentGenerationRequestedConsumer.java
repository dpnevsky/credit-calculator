package com.dpnevsky.creditcalculator.document.infrastructure.messaging;

import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerationRequested;
import com.dpnevsky.creditcalculator.contracts.eventenvelope.EventEnvelope;
import com.dpnevsky.creditcalculator.document.infrastructure.persistence.entity.DocumentRequestEntity;
import com.dpnevsky.creditcalculator.document.infrastructure.persistence.repository.DocumentRequestRepository;
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
public class DocumentGenerationRequestedConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentGenerationRequestedConsumer.class);
    private static final String RECEIVED_STATUS = "RECEIVED";

    private final ObjectMapper objectMapper;
    private final DocumentRequestRepository documentRequestRepository;

    public DocumentGenerationRequestedConsumer(
            ObjectMapper objectMapper,
            DocumentRequestRepository documentRequestRepository
    ) {
        this.objectMapper = objectMapper;
        this.documentRequestRepository = documentRequestRepository;
    }

    @Transactional
    @KafkaListener(
            topics = "document-generation-requested",
            groupId = "document-service"
    )
    public void consume(String rawMessage) {
        EventEnvelope<DocumentGenerationRequested> envelope = parse(rawMessage);
        DocumentGenerationRequested payload = envelope.payload();

        DocumentRequestEntity documentRequestEntity = new DocumentRequestEntity(
                UUID.randomUUID(),
                payload.requestId(),
                payload.applicationId(),
                payload.documentType(),
                payload.formats(),
                payload.templateCode(),
                payload.templateVersion(),
                payload.requestedByUserId(),
                payload.requestedAt(),
                payload.renderContext(),
                RECEIVED_STATUS,
                OffsetDateTime.now()
        );

        documentRequestRepository.save(documentRequestEntity);

        log.info(
                "Saved document request eventType={}, applicationId={}, requestId={}, status={}",
                envelope.eventType(),
                payload.applicationId(),
                payload.requestId(),
                RECEIVED_STATUS
        );
    }

    private EventEnvelope<DocumentGenerationRequested> parse(String rawMessage) {
        try {
            return objectMapper.readValue(
                    rawMessage,
                    new TypeReference<EventEnvelope<DocumentGenerationRequested>>() {}
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse DocumentGenerationRequested event", exception);
        }
    }
}