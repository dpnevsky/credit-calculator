package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.application.exception.ApplicationAccessDeniedException;
import com.dpnevsky.creditcalculator.application.application.exception.ApplicationNotFoundException;
import com.dpnevsky.creditcalculator.application.application.exception.ApplicationDocumentNotFoundException;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationDocumentEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationDocumentRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ApplicationAccessService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;

    public ApplicationAccessService(
            ApplicationRepository applicationRepository,
            ApplicationDocumentRepository applicationDocumentRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.applicationDocumentRepository = applicationDocumentRepository;
    }

    public ApplicationEntity getOwnedApplication(UUID applicationId, String userEmail) {
        return applicationRepository.findByIdAndEmail(applicationId, userEmail)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }

    public ApplicationEntity getOwnedApplicationForUpdate(UUID applicationId, String userEmail) {
        ApplicationEntity application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        if (!application.getEmail().equalsIgnoreCase(userEmail)) {
            throw new ApplicationAccessDeniedException(applicationId);
        }

        return application;
    }

    public ApplicationDocumentEntity getOwnedDocument(UUID documentId, String userEmail) {
        ApplicationDocumentEntity document = applicationDocumentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ApplicationDocumentNotFoundException(documentId));

        getOwnedApplication(document.getApplicationId(), userEmail);
        return document;
    }
}
