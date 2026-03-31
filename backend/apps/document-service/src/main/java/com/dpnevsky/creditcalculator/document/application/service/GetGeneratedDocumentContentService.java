package com.dpnevsky.creditcalculator.document.application.service;

import com.dpnevsky.creditcalculator.document.application.port.out.DocumentStoragePort;
import com.dpnevsky.creditcalculator.document.infrastructure.persistence.entity.GeneratedDocumentEntity;
import com.dpnevsky.creditcalculator.document.infrastructure.persistence.repository.GeneratedDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetGeneratedDocumentContentService {

    private static final String GENERATED_STATUS = "GENERATED";

    private final GeneratedDocumentRepository generatedDocumentRepository;
    private final DocumentStoragePort documentStoragePort;

    public GetGeneratedDocumentContentService(
            GeneratedDocumentRepository generatedDocumentRepository,
            DocumentStoragePort documentStoragePort
    ) {
        this.generatedDocumentRepository = generatedDocumentRepository;
        this.documentStoragePort = documentStoragePort;
    }

    public GeneratedDocumentContent getByDocumentId(UUID documentId) {
        GeneratedDocumentEntity document = generatedDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Generated document not found. documentId=" + documentId
                ));

        if (!GENERATED_STATUS.equals(document.getStatus())) {
            throw new IllegalStateException(
                    "Generated document is not ready for reading. documentId=" + documentId
                            + ", status=" + document.getStatus()
            );
        }

        DocumentStoragePort.StoredDocument storedDocument =
                documentStoragePort.load(document.getStorageKey());

        return new GeneratedDocumentContent(
                document.getId(),
                document.getApplicationId(),
                document.getFileName(),
                document.getMimeType(),
                document.getStorageKey(),
                storedDocument.content()
        );
    }

    public record GeneratedDocumentContent(
            UUID documentId,
            UUID applicationId,
            String fileName,
            String mimeType,
            String storageKey,
            byte[] content
    ) {
    }
}