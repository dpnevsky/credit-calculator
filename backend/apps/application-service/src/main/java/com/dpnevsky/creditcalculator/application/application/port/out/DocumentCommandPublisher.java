package com.dpnevsky.creditcalculator.application.application.port.out;

import java.util.UUID;

public interface DocumentCommandPublisher {

    void publishDocumentGenerationRequested(UUID applicationId);
}