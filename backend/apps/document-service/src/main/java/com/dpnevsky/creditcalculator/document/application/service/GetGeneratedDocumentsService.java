package com.dpnevsky.creditcalculator.document.application.service;

import com.dpnevsky.creditcalculator.document.api.rest.dto.GetGeneratedDocumentResponse;
import com.dpnevsky.creditcalculator.document.infrastructure.persistence.repository.GeneratedDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetGeneratedDocumentsService {

    private final GeneratedDocumentRepository generatedDocumentRepository;

    public GetGeneratedDocumentsService(GeneratedDocumentRepository generatedDocumentRepository) {
        this.generatedDocumentRepository = generatedDocumentRepository;
    }

    public List<GetGeneratedDocumentResponse> getByApplicationId(UUID applicationId) {
        return generatedDocumentRepository.findAllByApplicationIdOrderByGeneratedAtDesc(applicationId)
                .stream()
                .map(document -> new GetGeneratedDocumentResponse(
                        document.getId(),
                        document.getRequestId(),
                        document.getApplicationId(),
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