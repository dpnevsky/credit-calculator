package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.RequestDocumentsResponse;
import com.dpnevsky.creditcalculator.application.application.model.ApplicationStatus;
import com.dpnevsky.creditcalculator.application.application.model.ContractStatus;
import com.dpnevsky.creditcalculator.application.application.port.out.DocumentCommandPublisher;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationDocumentEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.OfferEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationDocumentRepository;
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

    private static final String DEFAULT_PAYMENT_TYPE = "ANNUITY";
    private static final String DOCUMENT_TYPE = "CREDIT_AGREEMENT";
    private static final String DOCUMENT_TEMPLATE_CODE = "credit-agreement";
    private static final String DOCUMENT_TEMPLATE_VERSION = "v2";

    private final ApplicationAccessService applicationAccessService;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final ApplicationRepository applicationRepository;
    private final OfferRepository offerRepository;
    private final DocumentCommandPublisher documentCommandPublisher;

    public RequestDocumentsService(
            ApplicationAccessService applicationAccessService,
            ApplicationDocumentRepository applicationDocumentRepository,
            ApplicationRepository applicationRepository,
            OfferRepository offerRepository,
            DocumentCommandPublisher documentCommandPublisher
    ) {
        this.applicationAccessService = applicationAccessService;
        this.applicationDocumentRepository = applicationDocumentRepository;
        this.applicationRepository = applicationRepository;
        this.offerRepository = offerRepository;
        this.documentCommandPublisher = documentCommandPublisher;
    }

    @Transactional
    public RequestDocumentsResponse requestDocuments(UUID applicationId, String userEmail, String paymentType) {
        ApplicationEntity existingApplication = applicationAccessService.getOwnedApplication(applicationId, userEmail);
        ensureDocumentsRequestAllowed(existingApplication.getStatus());

        ApplicationDocumentEntity existingDocument = applicationDocumentRepository
                .findFirstByApplicationIdAndDocumentTypeOrderByGeneratedAtDesc(applicationId, DOCUMENT_TYPE)
                .orElse(null);

        if (existingDocument != null) {
            ensureContractReadyState(existingApplication);
            return new RequestDocumentsResponse(
                    applicationId,
                    ApplicationStatus.DOCUMENTS_READY.name(),
                    "Credit agreement already exists"
            );
        }

        if (ApplicationStatus.DOCUMENTS_REQUESTED.name().equals(existingApplication.getStatus())) {
            return new RequestDocumentsResponse(
                    applicationId,
                    ApplicationStatus.DOCUMENTS_REQUESTED.name(),
                    "Document generation is already in progress"
            );
        }

        if (ApplicationStatus.DOCUMENTS_READY.name().equals(existingApplication.getStatus())) {
            return new RequestDocumentsResponse(
                    applicationId,
                    ApplicationStatus.DOCUMENTS_READY.name(),
                    "Credit agreement is already being finalized"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();
        String resolvedPaymentType = resolvePaymentType(existingApplication, paymentType);
        OfferEntity selectedOffer = getSelectedOffer(applicationId);

        applicationRepository.save(buildRequestedDocumentsApplication(
                existingApplication,
                resolvedPaymentType,
                ApplicationStatus.DOCUMENTS_REQUESTED.name(),
                now
        ));
        documentCommandPublisher.publishDocumentGenerationRequested(
                buildDocumentGenerationRequested(existingApplication, selectedOffer, resolvedPaymentType, userEmail, now)
        );

        return new RequestDocumentsResponse(
                applicationId,
                ApplicationStatus.DOCUMENTS_REQUESTED.name(),
                "Document generation has been requested"
        );
    }

    private void ensureDocumentsRequestAllowed(String currentStatus) {
        if (!ApplicationStatus.OFFER_SELECTED.name().equals(currentStatus)
                && !ApplicationStatus.DOCUMENTS_REQUESTED.name().equals(currentStatus)
                && !ApplicationStatus.DOCUMENTS_READY.name().equals(currentStatus)) {
            throw new IllegalStateException(
                    "Documents can be requested only for applications with status OFFER_SELECTED, DOCUMENTS_REQUESTED or DOCUMENTS_READY"
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
                resolvedPaymentType,
                existingApplication.getContractStatus(),
                existingApplication.getContractSignedAt(),
                existingApplication.getSignatureId()
        );
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
        if (application.getPaymentType() != null && !application.getPaymentType().isBlank()) {
            return application.getPaymentType();
        }
        if (requestedPaymentType != null && !requestedPaymentType.isBlank()) {
            return requestedPaymentType;
        }
        return DEFAULT_PAYMENT_TYPE;
    }

    private void ensureContractReadyState(ApplicationEntity application) {
        if (ContractStatus.SIGNED.name().equals(application.getContractStatus())) {
            return;
        }

        if (ContractStatus.READY_TO_SIGN.name().equals(application.getContractStatus())
                && ApplicationStatus.DOCUMENTS_READY.name().equals(application.getStatus())) {
            return;
        }

        application.setContractStatus(ContractStatus.READY_TO_SIGN.name());
        application.setStatus(ApplicationStatus.DOCUMENTS_READY.name());
        application.setUpdatedAt(OffsetDateTime.now());
        applicationRepository.save(application);
    }
}
