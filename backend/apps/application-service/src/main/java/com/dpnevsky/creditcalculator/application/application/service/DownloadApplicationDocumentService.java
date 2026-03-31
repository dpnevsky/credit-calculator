package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.application.port.out.DocumentDownloadClient;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationDocumentEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DownloadApplicationDocumentService {

    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final DocumentDownloadClient documentDownloadClient;

    public DownloadApplicationDocumentService(
            ApplicationDocumentRepository applicationDocumentRepository,
            DocumentDownloadClient documentDownloadClient
    ) {
        this.applicationDocumentRepository = applicationDocumentRepository;
        this.documentDownloadClient = documentDownloadClient;
    }

    public DownloadedApplicationDocument downloadByDocumentId(UUID documentId) {
        ApplicationDocumentEntity applicationDocument = applicationDocumentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Application document not found. documentId=" + documentId
                ));

        DocumentDownloadClient.DownloadedDocument downloadedDocument =
                documentDownloadClient.downloadByDocumentId(documentId);

        return new DownloadedApplicationDocument(
                applicationDocument.getApplicationId(),
                downloadedDocument.documentId(),
                downloadedDocument.fileName(),
                downloadedDocument.mimeType(),
                downloadedDocument.content()
        );
    }

    public record DownloadedApplicationDocument(
            UUID applicationId,
            UUID documentId,
            String fileName,
            String mimeType,
            byte[] content
    ) {
    }
}