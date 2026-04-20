package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.application.port.out.DocumentDownloadClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DownloadApplicationDocumentService {

    private final ApplicationAccessService applicationAccessService;
    private final DocumentDownloadClient documentDownloadClient;

    public DownloadApplicationDocumentService(
            ApplicationAccessService applicationAccessService,
            DocumentDownloadClient documentDownloadClient
    ) {
        this.applicationAccessService = applicationAccessService;
        this.documentDownloadClient = documentDownloadClient;
    }

    public DownloadedApplicationDocument downloadByDocumentId(UUID documentId, String userEmail) {
        var applicationDocument = applicationAccessService.getOwnedDocument(documentId, userEmail);

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
