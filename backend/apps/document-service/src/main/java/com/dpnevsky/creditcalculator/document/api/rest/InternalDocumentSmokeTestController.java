package com.dpnevsky.creditcalculator.document.api.rest;

import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerated;
import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerationRequested;
import com.dpnevsky.creditcalculator.document.application.service.DocumentGenerationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/test/documents")
public class InternalDocumentSmokeTestController {

    private final DocumentGenerationService documentGenerationService;

    public InternalDocumentSmokeTestController(DocumentGenerationService documentGenerationService) {
        this.documentGenerationService = documentGenerationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<DocumentGenerated> generate(
            @Valid @RequestBody DocumentGenerationRequested request
    ) {
        return ResponseEntity.ok(documentGenerationService.generate(request));
    }
}

