package com.dpnevsky.creditcalculator.application.application.model;

public enum ApplicationStatus {
    DRAFT,
    PRESCORING_REJECTED,
    SUBMITTED,
    SCORING_COMPLETED,
    SCORING_APPROVED,
    SCORING_REJECTED,
    OFFER_SELECTED,
    DOCUMENTS_REQUESTED,
    DOCUMENTS_READY,
    CONTRACT_SIGNED
}
