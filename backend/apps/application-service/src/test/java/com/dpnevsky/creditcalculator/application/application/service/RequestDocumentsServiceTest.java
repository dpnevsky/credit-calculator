package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.application.port.out.DocumentCommandPublisher;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationDocumentEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.OfferEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationDocumentRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.OfferRepository;
import com.dpnevsky.creditcalculator.contracts.document.events.DocumentGenerationRequested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestDocumentsServiceTest {

    @Test
    void publishesGenerationOnlyForFirstExplicitRequest() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationDocumentRepository applicationDocumentRepository = mock(ApplicationDocumentRepository.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        DocumentCommandPublisher documentCommandPublisher = mock(DocumentCommandPublisher.class);
        RequestDocumentsService service = new RequestDocumentsService(
                applicationAccessService,
                applicationDocumentRepository,
                applicationRepository,
                offerRepository,
                documentCommandPublisher
        );
        UUID applicationId = UUID.randomUUID();
        OfferEntity selectedOffer = buildOffer(applicationId);

        when(applicationAccessService.getOwnedApplication(applicationId, "owner@example.com"))
                .thenReturn(buildApplication(applicationId, "OFFER_SELECTED", "DIFFERENTIAL"));
        when(applicationDocumentRepository.findFirstByApplicationIdAndDocumentTypeOrderByGeneratedAtDesc(
                applicationId,
                "CREDIT_AGREEMENT"
        )).thenReturn(Optional.empty());
        when(offerRepository.findFirstByApplicationIdAndSelectedTrue(applicationId)).thenReturn(Optional.of(selectedOffer));

        var response = service.requestDocuments(applicationId, "owner@example.com", "ANNUITY");

        assertEquals("DOCUMENTS_REQUESTED", response.status());
        verify(applicationRepository).save(argThat(application ->
                "DOCUMENTS_REQUESTED".equals(application.getStatus())
                        && "DIFFERENTIAL".equals(application.getPaymentType())
        ));

        ArgumentCaptor<DocumentGenerationRequested> requestCaptor =
                ArgumentCaptor.forClass(DocumentGenerationRequested.class);
        verify(documentCommandPublisher).publishDocumentGenerationRequested(requestCaptor.capture());
        assertEquals("DIFFERENTIAL", requestCaptor.getValue().creditAgreementData().paymentType());
        assertEquals("owner@example.com", requestCaptor.getValue().requestedByUserId());
    }

    @Test
    void doesNotRepublishWhileGenerationIsAlreadyInProgress() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationDocumentRepository applicationDocumentRepository = mock(ApplicationDocumentRepository.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        DocumentCommandPublisher documentCommandPublisher = mock(DocumentCommandPublisher.class);
        RequestDocumentsService service = new RequestDocumentsService(
                applicationAccessService,
                applicationDocumentRepository,
                applicationRepository,
                offerRepository,
                documentCommandPublisher
        );
        UUID applicationId = UUID.randomUUID();

        when(applicationAccessService.getOwnedApplication(applicationId, "owner@example.com"))
                .thenReturn(buildApplication(applicationId, "DOCUMENTS_REQUESTED", "ANNUITY"));
        when(applicationDocumentRepository.findFirstByApplicationIdAndDocumentTypeOrderByGeneratedAtDesc(
                applicationId,
                "CREDIT_AGREEMENT"
        )).thenReturn(Optional.empty());

        var response = service.requestDocuments(applicationId, "owner@example.com", null);

        assertEquals("DOCUMENTS_REQUESTED", response.status());
        assertEquals("Document generation is already in progress", response.message());
        verify(applicationRepository, never()).save(any());
        verify(documentCommandPublisher, never()).publishDocumentGenerationRequested(any());
        verify(offerRepository, never()).findFirstByApplicationIdAndSelectedTrue(any());
    }

    @Test
    void keepsReadyStatusWhenAgreementAlreadyExists() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationDocumentRepository applicationDocumentRepository = mock(ApplicationDocumentRepository.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        DocumentCommandPublisher documentCommandPublisher = mock(DocumentCommandPublisher.class);
        RequestDocumentsService service = new RequestDocumentsService(
                applicationAccessService,
                applicationDocumentRepository,
                applicationRepository,
                offerRepository,
                documentCommandPublisher
        );
        UUID applicationId = UUID.randomUUID();

        when(applicationAccessService.getOwnedApplication(applicationId, "owner@example.com"))
                .thenReturn(buildApplication(applicationId, "DOCUMENTS_READY", "ANNUITY"));
        when(applicationDocumentRepository.findFirstByApplicationIdAndDocumentTypeOrderByGeneratedAtDesc(
                applicationId,
                "CREDIT_AGREEMENT"
        )).thenReturn(Optional.of(buildDocument(applicationId)));

        var response = service.requestDocuments(applicationId, "owner@example.com", "DIFFERENTIAL");

        assertEquals("DOCUMENTS_READY", response.status());
        assertEquals("Credit agreement already exists", response.message());
        verify(applicationRepository, never()).save(any());
        verify(documentCommandPublisher, never()).publishDocumentGenerationRequested(any());
        verify(offerRepository, never()).findFirstByApplicationIdAndSelectedTrue(any());
    }

    private ApplicationEntity buildApplication(UUID applicationId, String status, String paymentType) {
        OffsetDateTime now = OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC);
        return new ApplicationEntity(
                applicationId,
                status,
                new BigDecimal("500000.00"),
                12,
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "owner@example.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "567890",
                now,
                now,
                paymentType
        );
    }

    private OfferEntity buildOffer(UUID applicationId) {
        return new OfferEntity(
                UUID.randomUUID(),
                applicationId,
                new BigDecimal("500000.00"),
                new BigDecimal("540000.00"),
                12,
                new BigDecimal("45000.00"),
                new BigDecimal("12.00"),
                false,
                false,
                true,
                OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC)
        );
    }

    private ApplicationDocumentEntity buildDocument(UUID applicationId) {
        OffsetDateTime now = OffsetDateTime.of(2026, 4, 17, 13, 0, 0, 0, ZoneOffset.UTC);
        return new ApplicationDocumentEntity(
                UUID.randomUUID(),
                applicationId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CREDIT_AGREEMENT",
                "PDF",
                "credit-agreement.pdf",
                "application/pdf",
                "documents/credit-agreement.pdf",
                "GENERATED",
                now,
                now
        );
    }
}
