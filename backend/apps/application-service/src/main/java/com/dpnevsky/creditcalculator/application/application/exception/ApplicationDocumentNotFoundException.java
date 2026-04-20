package com.dpnevsky.creditcalculator.application.application.exception;

import java.util.UUID;

public class ApplicationDocumentNotFoundException extends RuntimeException {

    public ApplicationDocumentNotFoundException(UUID documentId) {
        super("Application document not found: " + documentId);
    }
}
