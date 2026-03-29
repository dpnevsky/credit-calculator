package com.dpnevsky.creditcalculator.application.api.rest;

import com.dpnevsky.creditcalculator.application.api.rest.dto.CreateApplicationRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.CreateApplicationResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationDocumentResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationScoringResultResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.RequestDocumentsResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.SubmitApplicationRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.SubmitApplicationResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.UpdateApplicationRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.UpdateApplicationResponse;
import com.dpnevsky.creditcalculator.application.application.service.CreateApplicationService;
import com.dpnevsky.creditcalculator.application.application.service.GetApplicationDocumentsService;
import com.dpnevsky.creditcalculator.application.application.service.GetApplicationScoringResultService;
import com.dpnevsky.creditcalculator.application.application.service.GetApplicationService;
import com.dpnevsky.creditcalculator.application.application.service.RequestDocumentsService;
import com.dpnevsky.creditcalculator.application.application.service.SubmitApplicationService;
import com.dpnevsky.creditcalculator.application.application.service.UpdateApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ApplicationController {

    private final CreateApplicationService createApplicationService;
    private final GetApplicationService getApplicationService;
    private final UpdateApplicationService updateApplicationService;
    private final SubmitApplicationService submitApplicationService;
    private final GetApplicationScoringResultService getApplicationScoringResultService;
    private final RequestDocumentsService requestDocumentsService;
    private final GetApplicationDocumentsService getApplicationDocumentsService;

    public ApplicationController(
            CreateApplicationService createApplicationService,
            GetApplicationService getApplicationService,
            UpdateApplicationService updateApplicationService,
            SubmitApplicationService submitApplicationService,
            GetApplicationScoringResultService getApplicationScoringResultService,
            RequestDocumentsService requestDocumentsService,
            GetApplicationDocumentsService getApplicationDocumentsService
    ) {
        this.createApplicationService = createApplicationService;
        this.getApplicationService = getApplicationService;
        this.updateApplicationService = updateApplicationService;
        this.submitApplicationService = submitApplicationService;
        this.getApplicationScoringResultService = getApplicationScoringResultService;
        this.requestDocumentsService = requestDocumentsService;
        this.getApplicationDocumentsService = getApplicationDocumentsService;
    }

    @PostMapping("/api/applications")
    public CreateApplicationResponse createApplication(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        validateDebugHeader(debugAuthHeader);
        return createApplicationService.create(request);
    }

    @GetMapping("/api/applications/{applicationId}")
    public GetApplicationResponse getApplication(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @PathVariable UUID applicationId
    ) {
        validateDebugHeader(debugAuthHeader);
        return getApplicationService.getById(applicationId);
    }

    @PatchMapping("/api/applications/{applicationId}")
    public UpdateApplicationResponse updateApplication(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @PathVariable UUID applicationId,
            @Valid @RequestBody UpdateApplicationRequest request
    ) {
        validateDebugHeader(debugAuthHeader);
        return updateApplicationService.update(applicationId, request);
    }

    @PostMapping("/api/applications/{applicationId}/submit")
    public SubmitApplicationResponse submitApplication(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @PathVariable UUID applicationId,
            @Valid @RequestBody SubmitApplicationRequest request
    ) {
        validateDebugHeader(debugAuthHeader);
        return submitApplicationService.submit(applicationId, request);
    }

    @GetMapping("/api/applications/{applicationId}/scoring-result")
    public GetApplicationScoringResultResponse getApplicationScoringResult(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @PathVariable UUID applicationId
    ) {
        validateDebugHeader(debugAuthHeader);
        return getApplicationScoringResultService.getLatestByApplicationId(applicationId);
    }

    @PostMapping("/api/applications/{applicationId}/request-documents")
    public RequestDocumentsResponse requestDocuments(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @PathVariable UUID applicationId
    ) {
        validateDebugHeader(debugAuthHeader);
        return requestDocumentsService.requestDocuments(applicationId);
    }

    @GetMapping("/api/applications/{applicationId}/documents")
    public List<GetApplicationDocumentResponse> getApplicationDocuments(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @PathVariable UUID applicationId
    ) {
        validateDebugHeader(debugAuthHeader);
        return getApplicationDocumentsService.getByApplicationId(applicationId);
    }

    private void validateDebugHeader(String debugAuthHeader) {
        if (!"allow".equals(debugAuthHeader)) {
            throw new IllegalStateException("Missing or invalid X-Debug-Auth header");
        }
    }
}