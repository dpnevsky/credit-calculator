package com.dpnevsky.creditcalculator.document.application.service;

import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerated;
import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerationRequested;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DocumentGenerationService {

    private static final String GENERATED_STATUS = "GENERATED";

    public DocumentGenerated generate(DocumentGenerationRequested request) {
        String format = request.formats().get(0);
        String fileExtension = resolveFileExtension(format);
        String mimeType = resolveMimeType(format);

        UUID documentId = UUID.randomUUID();
        String fileName = request.documentType().toLowerCase() + "-" + request.applicationId() + "." + fileExtension;
        String storageKey = "applications/" + request.applicationId() + "/" + fileName;

        return new DocumentGenerated(
                request.requestId(),
                request.applicationId(),
                documentId,
                request.documentType(),
                format,
                fileName,
                mimeType,
                storageKey,
                GENERATED_STATUS,
                OffsetDateTime.now()
        );
    }

    private String resolveFileExtension(String format) {
        return switch (format) {
            case "PDF" -> "pdf";
            case "XML" -> "xml";
            default -> "bin";
        };
    }

    private String resolveMimeType(String format) {
        return switch (format) {
            case "PDF" -> "application/pdf";
            case "XML" -> "application/xml";
            default -> "application/octet-stream";
        };
    }
}