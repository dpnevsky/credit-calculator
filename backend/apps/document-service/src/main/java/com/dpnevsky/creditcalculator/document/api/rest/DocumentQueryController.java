package com.dpnevsky.creditcalculator.document.api.rest;

import com.dpnevsky.creditcalculator.document.api.rest.dto.GetGeneratedDocumentResponse;
import com.dpnevsky.creditcalculator.document.application.service.GetGeneratedDocumentContentService;
import com.dpnevsky.creditcalculator.document.application.service.GetGeneratedDocumentsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
public class DocumentQueryController {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final GetGeneratedDocumentsService getGeneratedDocumentsService;
    private final GetGeneratedDocumentContentService getGeneratedDocumentContentService;
    private final String internalApiKey;

    public DocumentQueryController(
            GetGeneratedDocumentsService getGeneratedDocumentsService,
            GetGeneratedDocumentContentService getGeneratedDocumentContentService,
            @Value("${document.internal.api-key}") String internalApiKey
    ) {
        this.getGeneratedDocumentsService = getGeneratedDocumentsService;
        this.getGeneratedDocumentContentService = getGeneratedDocumentContentService;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/internal/documents/application/{applicationId}")
    public List<GetGeneratedDocumentResponse> getDocumentsByApplicationId(
            @PathVariable UUID applicationId,
            @RequestHeader(name = INTERNAL_API_KEY_HEADER, required = false) String requestInternalApiKey
    ) {
        validateInternalApiKey(requestInternalApiKey);
        return getGeneratedDocumentsService.getByApplicationId(applicationId);
    }

    @GetMapping("/internal/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocumentById(
            @PathVariable UUID documentId,
            @RequestHeader(name = INTERNAL_API_KEY_HEADER, required = false) String requestInternalApiKey
    ) {
        validateInternalApiKey(requestInternalApiKey);
        GetGeneratedDocumentContentService.GeneratedDocumentContent document =
                getGeneratedDocumentContentService.getByDocumentId(documentId);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (document.mimeType() != null && !document.mimeType().isBlank()) {
            mediaType = MediaType.parseMediaType(document.mimeType());
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(document.fileName())
                                .build()
                                .toString()
                )
                .body(document.content());
    }

    private void validateInternalApiKey(String requestInternalApiKey) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalStateException("document.internal.api-key must be configured");
        }
        if (!internalApiKey.equals(requestInternalApiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }
}
