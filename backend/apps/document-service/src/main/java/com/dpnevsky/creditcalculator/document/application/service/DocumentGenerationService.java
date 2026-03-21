package com.dpnevsky.creditcalculator.document.application.service;

import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerated;
import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerationRequested;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DocumentGenerationService {

    public DocumentGenerated generate(DocumentGenerationRequested request) {
        String primaryFormat = request.formats().isEmpty() ? "PDF" : request.formats().get(0);

        return new DocumentGenerated(
                request.requestId(),
                request.applicationId(),
                UUID.randomUUID(),
                request.documentType(),
                primaryFormat,
                buildFileName(request.documentType(), request.applicationId(), primaryFormat),
                resolveMimeType(primaryFormat),
                "FILESYSTEM",
                buildStorageKey(request.applicationId(), request.documentType(), primaryFormat),
                1024L,
                "stub-checksum-sha256",
                OffsetDateTime.now()
        );
    }

    private String buildFileName(String documentType, UUID applicationId, String format) {
        return documentType.toLowerCase() + "-" + applicationId + "." + format.toLowerCase();
    }

    private String buildStorageKey(UUID applicationId, String documentType, String format) {
        return "applications/" + applicationId + "/" + documentType.toLowerCase() + "." + format.toLowerCase();
    }

    private String resolveMimeType(String format) {
        return switch (format.toUpperCase()) {
            case "PDF" -> "application/pdf";
            case "XML" -> "application/xml";
            default -> "application/octet-stream";
        };
    }
}