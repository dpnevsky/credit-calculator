package com.dpnevsky.creditcalculator.document.api.rest;

import com.dpnevsky.creditcalculator.document.api.rest.dto.GetGeneratedDocumentResponse;
import com.dpnevsky.creditcalculator.document.application.service.GetGeneratedDocumentContentService;
import com.dpnevsky.creditcalculator.document.application.service.GetGeneratedDocumentsService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class DocumentQueryController {

    private final GetGeneratedDocumentsService getGeneratedDocumentsService;
    private final GetGeneratedDocumentContentService getGeneratedDocumentContentService;

    public DocumentQueryController(
            GetGeneratedDocumentsService getGeneratedDocumentsService,
            GetGeneratedDocumentContentService getGeneratedDocumentContentService
    ) {
        this.getGeneratedDocumentsService = getGeneratedDocumentsService;
        this.getGeneratedDocumentContentService = getGeneratedDocumentContentService;
    }

    @GetMapping("/internal/documents/application/{applicationId}")
    public List<GetGeneratedDocumentResponse> getDocumentsByApplicationId(@PathVariable UUID applicationId) {
        return getGeneratedDocumentsService.getByApplicationId(applicationId);
    }

    @GetMapping("/internal/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocumentById(@PathVariable UUID documentId) {
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
}
