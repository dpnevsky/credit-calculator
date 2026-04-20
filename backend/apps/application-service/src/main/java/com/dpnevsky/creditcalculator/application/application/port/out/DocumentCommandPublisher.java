package com.dpnevsky.creditcalculator.application.application.port.out;

import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerationRequested;

public interface DocumentCommandPublisher {

    void publishDocumentGenerationRequested(DocumentGenerationRequested request);
}
