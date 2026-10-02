package com.dpnevsky.creditcalculator.application.infrastructure.messaging;

import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerationRequested;
import com.dpnevsky.creditcalculator.contracts.eventenvelope.EventEnvelope;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaDocumentCommandPublisherTest {
    @Test
    @SuppressWarnings("unchecked")
    void propagatesBrokerFailureToTheTransaction() {
        KafkaTemplate<String, EventEnvelope<DocumentGenerationRequested>> kafka = mock(KafkaTemplate.class);
        when(kafka.send(anyString(), anyString(), any(EventEnvelope.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Broker unavailable")));
        UUID id = UUID.randomUUID();
        var event = new DocumentGenerationRequested(UUID.randomUUID(), id, "CREDIT_AGREEMENT", List.of("PDF"), "credit-agreement", "v2", "owner@example.com", null, OffsetDateTime.now(), Map.of());

        assertThrows(IllegalStateException.class, () -> new KafkaDocumentCommandPublisher(kafka).publishDocumentGenerationRequested(event));
    }
}
