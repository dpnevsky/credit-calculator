package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationDocumentResponse;
import com.dpnevsky.creditcalculator.application.application.exception.ApplicationNotFoundException;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationDocumentRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetApplicationDocumentsService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;

    public GetApplicationDocumentsService(
            ApplicationRepository applicationRepository,
            ApplicationDocumentRepository applicationDocumentRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.applicationDocumentRepository = applicationDocumentRepository;
    }

    public List<GetApplicationDocumentResponse> getByApplicationId(UUID applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ApplicationNotFoundException(applicationId);
        }

        return applicationDocumentRepository.findAllByApplicationIdOrderByGeneratedAtDesc(applicationId)
                .stream()
                .map(document -> new GetApplicationDocumentResponse(
                        document.getId(),
                        document.getApplicationId(),
                        document.getRequestId(),
                        document.getDocumentId(),
                        document.getDocumentType(),
                        document.getFormat(),
                        document.getFileName(),
                        document.getMimeType(),
                        document.getStorageKey(),
                        document.getStatus(),
                        document.getGeneratedAt()
                ))
                .toList();
    }
}