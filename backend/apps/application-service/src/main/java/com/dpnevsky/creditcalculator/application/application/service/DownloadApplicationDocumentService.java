package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.application.port.out.DocumentDownloadClient;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationDocumentEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DownloadApplicationDocumentService {

    private static final String CONTRACT_FILE_NAME = "credit-agreement-payment-schedule.pdf";

    private final ApplicationAccessService applicationAccessService;
    private final DocumentDownloadClient documentDownloadClient;
    private final GenerateApplicationContractPdfService generateApplicationContractPdfService;

    public DownloadApplicationDocumentService(
            ApplicationAccessService applicationAccessService,
            DocumentDownloadClient documentDownloadClient,
            GenerateApplicationContractPdfService generateApplicationContractPdfService
    ) {
        this.applicationAccessService = applicationAccessService;
        this.documentDownloadClient = documentDownloadClient;
        this.generateApplicationContractPdfService = generateApplicationContractPdfService;
    }

    public DownloadedApplicationDocument downloadByDocumentId(UUID documentId, String userEmail) {
        ApplicationDocumentEntity applicationDocument = applicationAccessService.getOwnedDocument(documentId, userEmail);

        if (generateApplicationContractPdfService.supports(
                applicationDocument.getDocumentType(),
                applicationDocument.getFormat()
        )) {
            return new DownloadedApplicationDocument(
                    applicationDocument.getApplicationId(),
                    applicationDocument.getDocumentId(),
                    CONTRACT_FILE_NAME,
                    applicationDocument.getMimeType(),
                    generateApplicationContractPdfService.generate(
                            applicationDocument.getApplicationId(),
                            applicationDocument.getGeneratedAt()
                    )
            );
        }

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
