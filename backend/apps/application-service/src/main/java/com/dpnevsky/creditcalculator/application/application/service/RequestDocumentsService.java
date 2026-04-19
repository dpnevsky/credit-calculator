package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.RequestDocumentsResponse;
import com.dpnevsky.creditcalculator.application.application.port.out.DocumentCommandPublisher;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RequestDocumentsService {

    private static final String SCORING_COMPLETED_STATUS = "SCORING_COMPLETED";
    private static final String OFFER_SELECTED_STATUS = "OFFER_SELECTED";
    private static final String DOCUMENTS_REQUESTED_STATUS = "DOCUMENTS_REQUESTED";
    private static final String DOCUMENTS_READY_STATUS = "DOCUMENTS_READY";
    private static final String DEFAULT_PAYMENT_TYPE = "ANNUITY";

    private final ApplicationAccessService applicationAccessService;
    private final ApplicationRepository applicationRepository;
    private final DocumentCommandPublisher documentCommandPublisher;

    public RequestDocumentsService(
            ApplicationAccessService applicationAccessService,
            ApplicationRepository applicationRepository,
            DocumentCommandPublisher documentCommandPublisher
    ) {
        this.applicationAccessService = applicationAccessService;
        this.applicationRepository = applicationRepository;
        this.documentCommandPublisher = documentCommandPublisher;
    }

    @Transactional
    public RequestDocumentsResponse requestDocuments(UUID applicationId, String userEmail, String paymentType) {
        ApplicationEntity existingApplication = applicationAccessService.getOwnedApplication(applicationId, userEmail);
        ensureDocumentsRequestAllowed(existingApplication.getStatus());

        OffsetDateTime now = OffsetDateTime.now();
        String resolvedPaymentType = resolvePaymentType(existingApplication, paymentType);
        String nextStatus = resolveNextStatus(existingApplication.getStatus());

        applicationRepository.save(buildRequestedDocumentsApplication(
                existingApplication,
                resolvedPaymentType,
                nextStatus,
                now
        ));
        documentCommandPublisher.publishDocumentGenerationRequested(applicationId);

        return new RequestDocumentsResponse(
                applicationId,
                nextStatus,
                "Document generation has been requested"
        );
    }

    private void ensureDocumentsRequestAllowed(String currentStatus) {
        if (!SCORING_COMPLETED_STATUS.equals(currentStatus)
                && !OFFER_SELECTED_STATUS.equals(currentStatus)
                && !DOCUMENTS_REQUESTED_STATUS.equals(currentStatus)
                && !DOCUMENTS_READY_STATUS.equals(currentStatus)) {
            throw new IllegalStateException(
                    "Documents can be requested only for applications with status SCORING_COMPLETED, OFFER_SELECTED, DOCUMENTS_REQUESTED or DOCUMENTS_READY"
            );
        }
    }

    private ApplicationEntity buildRequestedDocumentsApplication(
            ApplicationEntity existingApplication,
            String resolvedPaymentType,
            String nextStatus,
            OffsetDateTime updatedAt
    ) {
        return new ApplicationEntity(
                existingApplication.getId(),
                nextStatus,
                existingApplication.getAmount(),
                existingApplication.getTermMonths(),
                existingApplication.getFirstName(),
                existingApplication.getLastName(),
                existingApplication.getMiddleName(),
                existingApplication.getEmail(),
                existingApplication.getBirthDate(),
                existingApplication.getPassportSeries(),
                existingApplication.getPassportNumber(),
                existingApplication.getCreatedAt(),
                updatedAt,
                resolvedPaymentType
        );
    }

    private String resolveNextStatus(String currentStatus) {
        return DOCUMENTS_READY_STATUS.equals(currentStatus)
                ? DOCUMENTS_READY_STATUS
                : DOCUMENTS_REQUESTED_STATUS;
    }

    private String resolvePaymentType(ApplicationEntity application, String requestedPaymentType) {
        if (requestedPaymentType != null && !requestedPaymentType.isBlank()) {
            return requestedPaymentType;
        }
        if (application.getPaymentType() != null && !application.getPaymentType().isBlank()) {
            return application.getPaymentType();
        }
        return DEFAULT_PAYMENT_TYPE;
    }
}
