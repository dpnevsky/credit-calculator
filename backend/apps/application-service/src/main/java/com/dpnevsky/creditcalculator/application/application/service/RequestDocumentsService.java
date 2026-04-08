package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.RequestDocumentsResponse;
import com.dpnevsky.creditcalculator.application.application.exception.ApplicationNotFoundException;
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

    private final ApplicationRepository applicationRepository;
    private final DocumentCommandPublisher documentCommandPublisher;

    public RequestDocumentsService(
            ApplicationRepository applicationRepository,
            DocumentCommandPublisher documentCommandPublisher
    ) {
        this.applicationRepository = applicationRepository;
        this.documentCommandPublisher = documentCommandPublisher;
    }

    @Transactional
    public RequestDocumentsResponse requestDocuments(UUID applicationId) {
        ApplicationEntity existingApplication = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        String currentStatus = existingApplication.getStatus();

        if (DOCUMENTS_REQUESTED_STATUS.equals(currentStatus)) {
            return new RequestDocumentsResponse(
                    applicationId,
                    DOCUMENTS_REQUESTED_STATUS,
                    "Document generation has already been requested"
            );
        }

        if (!SCORING_COMPLETED_STATUS.equals(currentStatus) && !OFFER_SELECTED_STATUS.equals(currentStatus)) {
            throw new IllegalStateException(
                    "Documents can be requested only for applications with status SCORING_COMPLETED or OFFER_SELECTED"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        ApplicationEntity updatedApplication = new ApplicationEntity(
                existingApplication.getId(),
                DOCUMENTS_REQUESTED_STATUS,
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
                now
        );

        applicationRepository.save(updatedApplication);

        documentCommandPublisher.publishDocumentGenerationRequested(applicationId);

        return new RequestDocumentsResponse(
                applicationId,
                DOCUMENTS_REQUESTED_STATUS,
                "Document generation has been requested"
        );
    }
}
