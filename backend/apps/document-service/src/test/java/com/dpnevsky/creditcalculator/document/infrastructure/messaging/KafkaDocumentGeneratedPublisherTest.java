package com.dpnevsky.creditcalculator.document.infrastructure.messaging;

import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerated;
import com.dpnevsky.creditcalculator.contracts.eventenvelope.EventEnvelope;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaDocumentGeneratedPublisherTest {
    @Test
    @SuppressWarnings("unchecked")
    void propagatesBrokerFailureToTheTransaction() {
        KafkaTemplate<String, EventEnvelope<DocumentGenerated>> kafka = mock(KafkaTemplate.class);
        when(kafka.send(anyString(), anyString(), any(EventEnvelope.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Broker unavailable")));
        UUID id = UUID.randomUUID();
        var event = new DocumentGenerated(UUID.randomUUID(), id, UUID.randomUUID(), "CREDIT_AGREEMENT", "PDF", "agreement.pdf", "application/pdf", "agreement.pdf", "GENERATED", OffsetDateTime.now());

        assertThrows(IllegalStateException.class, () -> new KafkaDocumentGeneratedPublisher(kafka).publish(event));
    }
}
