package com.dpnevsky.creditcalculator.document.infrastructure.messaging;

import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerationRequested;
import com.dpnevsky.creditcalculator.contracts.eventenvelope.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentGenerationRequestedConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentGenerationRequestedConsumer.class);

    private final ObjectMapper objectMapper;

    public DocumentGenerationRequestedConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "document-generation-requested",
            groupId = "document-service"
    )
    public void consume(String rawMessage) {
        EventEnvelope<DocumentGenerationRequested> envelope = parse(rawMessage);

        log.info(
                "Consumed DocumentGenerationRequested eventType={}, applicationId={}, requestId={}",
                envelope.eventType(),
                envelope.payload().applicationId(),
                envelope.payload().requestId()
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