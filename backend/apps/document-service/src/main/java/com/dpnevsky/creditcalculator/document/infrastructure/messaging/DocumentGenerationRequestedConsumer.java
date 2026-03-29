package com.dpnevsky.creditcalculator.document.infrastructure.messaging;

import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerationRequested;
import com.dpnevsky.creditcalculator.contracts.eventenvelope.EventEnvelope;
import com.dpnevsky.creditcalculator.document.infrastructure.persistence.entity.DocumentRequestEntity;
import com.dpnevsky.creditcalculator.document.infrastructure.persistence.entity.GeneratedDocumentEntity;
import com.dpnevsky.creditcalculator.document.infrastructure.persistence.repository.DocumentRequestRepository;
import com.dpnevsky.creditcalculator.document.infrastructure.persistence.repository.GeneratedDocumentRepository;
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
    private static final String GENERATED_STATUS = "GENERATED";

    private final ObjectMapper objectMapper;
    private final DocumentRequestRepository documentRequestRepository;
    private final GeneratedDocumentRepository generatedDocumentRepository;

    public DocumentGenerationRequestedConsumer(
            ObjectMapper objectMapper,
            DocumentRequestRepository documentRequestRepository,
            GeneratedDocumentRepository generatedDocumentRepository
    ) {
        this.objectMapper = objectMapper;
        this.documentRequestRepository = documentRequestRepository;
        this.generatedDocumentRepository = generatedDocumentRepository;
    }

    @Transactional
    @KafkaListener(
            topics = "document-generation-requested",
            groupId = "document-service"
    )
    public void consume(String rawMessage) {
        EventEnvelope<DocumentGenerationRequested> envelope = parse(rawMessage);
        DocumentGenerationRequested payload = envelope.payload();

        if (documentRequestRepository.findByRequestId(payload.requestId()).isPresent()) {
            log.info(
                    "Skip duplicate DocumentGenerationRequested applicationId={}, requestId={}",
                    payload.applicationId(),
                    payload.requestId()
            );
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        DocumentRequestEntity receivedRequest = new DocumentRequestEntity(
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
                now
        );
        DocumentRequestEntity savedRequest = documentRequestRepository.save(receivedRequest);

        String format = payload.formats().get(0);
        String fileExtension = resolveFileExtension(format);
        String mimeType = resolveMimeType(format);
        String fileName = payload.documentType().toLowerCase() + "-" + payload.applicationId() + "." + fileExtension;
        String storageKey = "applications/" + payload.applicationId() + "/" + fileName;

        GeneratedDocumentEntity generatedDocumentEntity = new GeneratedDocumentEntity(
                UUID.randomUUID(),
                payload.requestId(),
                payload.applicationId(),
                payload.documentType(),
                format,
                fileName,
                mimeType,
                storageKey,
                GENERATED_STATUS,
                now
        );
        generatedDocumentRepository.save(generatedDocumentEntity);

        DocumentRequestEntity generatedRequest = new DocumentRequestEntity(
                savedRequest.getId(),
                savedRequest.getRequestId(),
                savedRequest.getApplicationId(),
                savedRequest.getDocumentType(),
                savedRequest.getFormatsJson(),
                savedRequest.getTemplateCode(),
                savedRequest.getTemplateVersion(),
                savedRequest.getRequestedByUserId(),
                savedRequest.getRequestedAt(),
                savedRequest.getRenderContextJson(),
                GENERATED_STATUS,
                savedRequest.getCreatedAt()
        );
        documentRequestRepository.save(generatedRequest);

        log.info(
                "Processed DocumentGenerationRequested applicationId={}, requestId={}, status={}",
                payload.applicationId(),
                payload.requestId(),
                GENERATED_STATUS
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

    private String resolveFileExtension(String format) {
        return switch (format) {
            case "PDF" -> "pdf";
            case "XML" -> "xml";
            default -> "bin";
        };
    }

    private String resolveMimeType(String format) {
        return switch (format) {
            case "PDF" -> "application/pdf";
            case "XML" -> "application/xml";
            default -> "application/octet-stream";
        };
    }
}