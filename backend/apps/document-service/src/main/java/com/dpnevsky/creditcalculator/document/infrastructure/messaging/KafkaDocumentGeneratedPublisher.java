package com.dpnevsky.creditcalculator.document.infrastructure.messaging;

import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerated;
import com.dpnevsky.creditcalculator.contracts.eventenvelope.EventEnvelope;
import com.dpnevsky.creditcalculator.document.application.port.out.DocumentGeneratedPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaDocumentGeneratedPublisher implements DocumentGeneratedPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaDocumentGeneratedPublisher.class);

    private static final String TOPIC = "document-generated";
    private static final String EVENT_TYPE = "DocumentGenerated";
    private static final Integer EVENT_VERSION = 1;
    private static final String PRODUCER = "document-service";

    private final KafkaTemplate<String, EventEnvelope<DocumentGenerated>> kafkaTemplate;

    public KafkaDocumentGeneratedPublisher(
            KafkaTemplate<String, EventEnvelope<DocumentGenerated>> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(DocumentGenerated event) {
        EventEnvelope<DocumentGenerated> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EVENT_TYPE,
                EVENT_VERSION,
                event.generatedAt(),
                PRODUCER,
                event.applicationId().toString(),
                null,
                event
        );

        kafkaTemplate.send(TOPIC, event.applicationId().toString(), envelope);

        log.info(
                "Published DocumentGenerated to topic={} applicationId={}, documentId={}",
                TOPIC,
                event.applicationId(),
                event.documentId()
        );
    }
}