//package com.dpnevsky.creditcalculator.application.infrastructure.messaging;
//
//import com.dpnevsky.creditcalculator.application.application.port.out.DocumentCommandPublisher;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//
//import java.util.UUID;
//
//@Component
//public class StubDocumentCommandPublisher implements DocumentCommandPublisher {
//
//    private static final Logger log = LoggerFactory.getLogger(StubDocumentCommandPublisher.class);
//
//    @Override
//    public void publishDocumentGenerationRequested(UUID applicationId) {
//        log.info("Stub publish DocumentGenerationRequested for applicationId={}", applicationId);
//    }
//}