package com.dpnevsky.creditcalculator.application.api.rest;

import com.dpnevsky.creditcalculator.application.api.rest.dto.CreateApplicationRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.CreateApplicationResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationDocumentResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationScoringResultResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.GetOfferResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.RequestDocumentsResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.SelectOfferResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.SubmitApplicationRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.SubmitApplicationResponse;
import com.dpnevsky.creditcalculator.application.api.rest.dto.UpdateApplicationRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.UpdateApplicationResponse;
import com.dpnevsky.creditcalculator.application.application.service.CreateApplicationService;
import com.dpnevsky.creditcalculator.application.application.service.DownloadApplicationDocumentService;
import com.dpnevsky.creditcalculator.application.application.service.GetApplicationDocumentsService;
import com.dpnevsky.creditcalculator.application.application.service.GetApplicationScoringResultService;
import com.dpnevsky.creditcalculator.application.application.service.GetApplicationsService;
import com.dpnevsky.creditcalculator.application.application.service.GetApplicationService;
import com.dpnevsky.creditcalculator.application.application.service.GetOffersService;
import com.dpnevsky.creditcalculator.application.application.service.RequestDocumentsService;
import com.dpnevsky.creditcalculator.application.application.service.SelectOfferService;
import com.dpnevsky.creditcalculator.application.application.service.SubmitApplicationService;
import com.dpnevsky.creditcalculator.application.application.service.UpdateApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ApplicationController {

    private final CreateApplicationService createApplicationService;
    private final GetApplicationService getApplicationService;
    private final GetApplicationsService getApplicationsService;
    private final UpdateApplicationService updateApplicationService;
    private final SubmitApplicationService submitApplicationService;
    private final GetApplicationScoringResultService getApplicationScoringResultService;
    private final RequestDocumentsService requestDocumentsService;
    private final GetApplicationDocumentsService getApplicationDocumentsService;
    private final DownloadApplicationDocumentService downloadApplicationDocumentService;
    private final GetOffersService getOffersService;
    private final SelectOfferService selectOfferService;

    public ApplicationController(
            CreateApplicationService createApplicationService,
            GetApplicationService getApplicationService,
            GetApplicationsService getApplicationsService,
            UpdateApplicationService updateApplicationService,
            SubmitApplicationService submitApplicationService,
            GetApplicationScoringResultService getApplicationScoringResultService,
            RequestDocumentsService requestDocumentsService,
            GetApplicationDocumentsService getApplicationDocumentsService,
            DownloadApplicationDocumentService downloadApplicationDocumentService,
            GetOffersService getOffersService,
            SelectOfferService selectOfferService
    ) {
        this.createApplicationService = createApplicationService;
        this.getApplicationService = getApplicationService;
        this.getApplicationsService = getApplicationsService;
        this.updateApplicationService = updateApplicationService;
        this.submitApplicationService = submitApplicationService;
        this.getApplicationScoringResultService = getApplicationScoringResultService;
        this.requestDocumentsService = requestDocumentsService;
        this.getApplicationDocumentsService = getApplicationDocumentsService;
        this.downloadApplicationDocumentService = downloadApplicationDocumentService;
        this.getOffersService = getOffersService;
        this.selectOfferService = selectOfferService;
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

    @GetMapping("/api/applications")
    public List<GetApplicationResponse> getApplications(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @RequestHeader(value = "X-User-Email", required = false) String userEmailHeader,
            @RequestParam(value = "email", required = false) String emailQueryParam
    ) {
        validateDebugHeader(debugAuthHeader);
        String userEmail = (emailQueryParam != null && !emailQueryParam.isBlank()) ? emailQueryParam : userEmailHeader;
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalStateException("Missing user email");
        }
        return getApplicationsService.getByEmail(userEmail);
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

    @GetMapping("/api/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @PathVariable UUID documentId
    ) {
        validateDebugHeader(debugAuthHeader);

        DownloadApplicationDocumentService.DownloadedApplicationDocument document =
                downloadApplicationDocumentService.downloadByDocumentId(documentId);

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

    @GetMapping("/api/applications/{applicationId}/offers")
    public List<GetOfferResponse> getOffers(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @PathVariable UUID applicationId
    ) {
        validateDebugHeader(debugAuthHeader);
        return getOffersService.getByApplicationId(applicationId);
    }

    @PostMapping("/api/applications/{applicationId}/offers/{offerId}/select")
    public SelectOfferResponse selectOffer(
            @RequestHeader(value = "X-Debug-Auth", required = false) String debugAuthHeader,
            @PathVariable UUID applicationId,
            @PathVariable UUID offerId
    ) {
        validateDebugHeader(debugAuthHeader);
        return selectOfferService.selectOffer(applicationId, offerId);
    }

    private void validateDebugHeader(String debugAuthHeader) {
        if (!"allow".equals(debugAuthHeader)) {
            throw new IllegalStateException("Missing or invalid X-Debug-Auth header");
        }
    }
}
