package com.dpnevsky.creditcalculator.application.infrastructure.messaging;

import com.dpnevsky.creditcalculator.application.application.port.out.DocumentCommandPublisher;
import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerationRequested;
import com.dpnevsky.creditcalculator.contracts.eventenvelope.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class KafkaDocumentCommandPublisher implements DocumentCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaDocumentCommandPublisher.class);

    private static final String TOPIC = "document-generation-requested";
    private static final String EVENT_TYPE = "DocumentGenerationRequested";
    private static final Integer EVENT_VERSION = 1;
    private static final String PRODUCER = "application-service";

    private final KafkaTemplate<String, EventEnvelope<DocumentGenerationRequested>> kafkaTemplate;

    public KafkaDocumentCommandPublisher(
            KafkaTemplate<String, EventEnvelope<DocumentGenerationRequested>> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishDocumentGenerationRequested(UUID applicationId) {
        OffsetDateTime now = OffsetDateTime.now();

        DocumentGenerationRequested payload = new DocumentGenerationRequested(
                UUID.randomUUID(),
                applicationId,
                "CREDIT_AGREEMENT",
                List.of("PDF"),
                "credit-agreement",
                "v1",
                "debug-user",
                now,
                Map.of(
                        "applicationId", applicationId.toString()
                )
        );

        EventEnvelope<DocumentGenerationRequested> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                EVENT_TYPE,
                EVENT_VERSION,
                now,
                PRODUCER,
                applicationId.toString(),
                null,
                payload
        );

        kafkaTemplate.send(TOPIC, applicationId.toString(), envelope);

        log.info("Published DocumentGenerationRequested to topic={} for applicationId={}", TOPIC, applicationId);
    }
}