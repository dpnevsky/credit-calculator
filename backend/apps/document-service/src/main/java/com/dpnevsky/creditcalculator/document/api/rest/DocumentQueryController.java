package com.dpnevsky.creditcalculator.document.api.rest;

import com.dpnevsky.creditcalculator.document.api.rest.dto.GetGeneratedDocumentResponse;
import com.dpnevsky.creditcalculator.document.application.service.GetGeneratedDocumentsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class DocumentQueryController {

    private final GetGeneratedDocumentsService getGeneratedDocumentsService;

    public DocumentQueryController(GetGeneratedDocumentsService getGeneratedDocumentsService) {
        this.getGeneratedDocumentsService = getGeneratedDocumentsService;
    }

    @GetMapping("/internal/documents/application/{applicationId}")
    public List<GetGeneratedDocumentResponse> getDocumentsByApplicationId(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @PathVariable UUID applicationId
    ) {
        if (!"allow".equals(debugAuthHeader)) {
            throw new IllegalStateException("Missing or invalid X-Debug-Auth header");
        }

        return getGeneratedDocumentsService.getByApplicationId(applicationId);
    }
}