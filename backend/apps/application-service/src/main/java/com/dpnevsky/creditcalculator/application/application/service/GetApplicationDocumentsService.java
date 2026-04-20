package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationDocumentResponse;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetApplicationDocumentsService {

    private final ApplicationAccessService applicationAccessService;
    private final ApplicationDocumentRepository applicationDocumentRepository;

    public GetApplicationDocumentsService(
            ApplicationAccessService applicationAccessService,
            ApplicationDocumentRepository applicationDocumentRepository
    ) {
        this.applicationAccessService = applicationAccessService;
        this.applicationDocumentRepository = applicationDocumentRepository;
    }

    public List<GetApplicationDocumentResponse> getByApplicationId(UUID applicationId, String userEmail) {
        applicationAccessService.getOwnedApplication(applicationId, userEmail);

        return applicationDocumentRepository.findAllByApplicationIdOrderByGeneratedAtDesc(applicationId)
                .stream()
                .map(document -> new GetApplicationDocumentResponse(
                        document.getDocumentId(),
                        document.getDocumentType(),
                        document.getFormat(),
                        document.getFileName(),
                        document.getMimeType(),
                        document.getStatus(),
                        document.getGeneratedAt()
                ))
                .toList();
    }
}
