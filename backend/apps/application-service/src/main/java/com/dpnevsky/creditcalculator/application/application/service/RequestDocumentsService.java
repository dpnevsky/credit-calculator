package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.RequestDocumentsResponse;
import com.dpnevsky.creditcalculator.application.application.port.out.DocumentCommandPublisher;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.OfferEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.OfferRepository;
import com.dpnevsky.creditcalculator.contracts.document.events.CreditAgreementRenderData;
import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerationRequested;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RequestDocumentsService {

    private static final String SCORING_COMPLETED_STATUS = "SCORING_COMPLETED";
    private static final String OFFER_SELECTED_STATUS = "OFFER_SELECTED";
    private static final String DOCUMENTS_REQUESTED_STATUS = "DOCUMENTS_REQUESTED";
    private static final String DOCUMENTS_READY_STATUS = "DOCUMENTS_READY";
    private static final String DEFAULT_PAYMENT_TYPE = "ANNUITY";
    private static final String DOCUMENT_TYPE = "CREDIT_AGREEMENT";
    private static final String DOCUMENT_TEMPLATE_CODE = "credit-agreement";
    private static final String DOCUMENT_TEMPLATE_VERSION = "v2";

    private final ApplicationAccessService applicationAccessService;
    private final ApplicationRepository applicationRepository;
    private final OfferRepository offerRepository;
    private final DocumentCommandPublisher documentCommandPublisher;

    public RequestDocumentsService(
            ApplicationAccessService applicationAccessService,
            ApplicationRepository applicationRepository,
            OfferRepository offerRepository,
            DocumentCommandPublisher documentCommandPublisher
    ) {
        this.applicationAccessService = applicationAccessService;
        this.applicationRepository = applicationRepository;
        this.offerRepository = offerRepository;
        this.documentCommandPublisher = documentCommandPublisher;
    }

    @Transactional
    public RequestDocumentsResponse requestDocuments(UUID applicationId, String userEmail, String paymentType) {
        ApplicationEntity existingApplication = applicationAccessService.getOwnedApplication(applicationId, userEmail);
        ensureDocumentsRequestAllowed(existingApplication.getStatus());

        OffsetDateTime now = OffsetDateTime.now();
        String resolvedPaymentType = resolvePaymentType(existingApplication, paymentType);
        String nextStatus = resolveNextStatus(existingApplication.getStatus());
        OfferEntity selectedOffer = getSelectedOffer(applicationId);

        applicationRepository.save(buildRequestedDocumentsApplication(
                existingApplication,
                resolvedPaymentType,
                nextStatus,
                now
        ));
        documentCommandPublisher.publishDocumentGenerationRequested(
                buildDocumentGenerationRequested(existingApplication, selectedOffer, resolvedPaymentType, userEmail, now)
        );

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

    private OfferEntity getSelectedOffer(UUID applicationId) {
        return offerRepository.findFirstByApplicationIdAndSelectedTrue(applicationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Selected offer is required before requesting credit agreement documents"
                ));
    }

    private DocumentGenerationRequested buildDocumentGenerationRequested(
            ApplicationEntity application,
            OfferEntity selectedOffer,
            String paymentType,
            String userEmail,
            OffsetDateTime requestedAt
    ) {
        return new DocumentGenerationRequested(
                UUID.randomUUID(),
                application.getId(),
                DOCUMENT_TYPE,
                List.of("PDF"),
                DOCUMENT_TEMPLATE_CODE,
                DOCUMENT_TEMPLATE_VERSION,
                userEmail,
                new CreditAgreementRenderData(
                        application.getFirstName(),
                        application.getLastName(),
                        application.getMiddleName(),
                        application.getPassportSeries(),
                        application.getPassportNumber(),
                        selectedOffer.getTotalAmount(),
                        selectedOffer.getTermMonths(),
                        selectedOffer.getRate(),
                        paymentType
                ),
                requestedAt,
                Map.of(
                        "applicationId", application.getId().toString(),
                        "paymentType", paymentType,
                        "selectedOfferId", selectedOffer.getId().toString()
                )
        );
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
