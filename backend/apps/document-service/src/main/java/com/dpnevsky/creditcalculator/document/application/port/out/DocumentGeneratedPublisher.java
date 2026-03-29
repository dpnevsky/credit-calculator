package com.dpnevsky.creditcalculator.document.application.port.out;

import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerated;

public interface DocumentGeneratedPublisher {

    void publish(DocumentGenerated event);
}