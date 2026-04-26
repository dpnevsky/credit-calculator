package com.dpnevsky.creditcalculator.application.application.exception;

import java.util.UUID;

public class ApplicationAccessDeniedException extends RuntimeException {

    public ApplicationAccessDeniedException(UUID applicationId) {
        super("Access denied for application: " + applicationId);
    }
}
