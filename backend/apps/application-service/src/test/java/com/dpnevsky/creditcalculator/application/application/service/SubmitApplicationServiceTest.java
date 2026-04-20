package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.SubmitApplicationRequest;
import com.dpnevsky.creditcalculator.application.application.port.out.ScoringClient;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationSubmitDataRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.OfferRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ScoringSnapshotRepository;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmitApplicationServiceTest {

    @Test
    void createsScoredOffersAfterApproval() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        ApplicationSubmitDataRepository submitDataRepository = mock(ApplicationSubmitDataRepository.class);
        ScoringSnapshotRepository scoringSnapshotRepository = mock(ScoringSnapshotRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        CreatePreliminaryOffersService createOffersService = mock(CreatePreliminaryOffersService.class);
        ScoringClient scoringClient = mock(ScoringClient.class);
        SubmitApplicationService service = new SubmitApplicationService(
                applicationAccessService,
                applicationRepository,
                submitDataRepository,
                scoringSnapshotRepository,
                offerRepository,
                createOffersService,
                scoringClient
        );
        UUID applicationId = UUID.randomUUID();

        when(applicationAccessService.getOwnedApplication(applicationId, "owner@example.com"))
                .thenReturn(buildApplication(applicationId, "DRAFT"));
        when(scoringClient.evaluate(any())).thenReturn(buildScoringResponse("APPROVED", List.of()));

        var response = service.submit(applicationId, "owner@example.com", buildSubmitRequest(false, ""));

        assertEquals("SCORING_COMPLETED", response.status());
        verify(createOffersService).create(
                applicationId,
                new BigDecimal("700000.00"),
                24,
                new BigDecimal("12.50")
        );
        verify(offerRepository, never()).deleteAllByApplicationId(any());
        verify(submitDataRepository).save(argThat(entity -> entity.getAccountNumber() == null));
    }

    @Test
    void removesOffersAfterRejection() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        ApplicationSubmitDataRepository submitDataRepository = mock(ApplicationSubmitDataRepository.class);
        ScoringSnapshotRepository scoringSnapshotRepository = mock(ScoringSnapshotRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        CreatePreliminaryOffersService createOffersService = mock(CreatePreliminaryOffersService.class);
        ScoringClient scoringClient = mock(ScoringClient.class);
        SubmitApplicationService service = new SubmitApplicationService(
                applicationAccessService,
                applicationRepository,
                submitDataRepository,
                scoringSnapshotRepository,
                offerRepository,
                createOffersService,
                scoringClient
        );
        UUID applicationId = UUID.randomUUID();

        when(applicationAccessService.getOwnedApplication(applicationId, "owner@example.com"))
                .thenReturn(buildApplication(applicationId, "DRAFT"));
        when(scoringClient.evaluate(any())).thenReturn(buildScoringResponse(
                "REJECTED",
                List.of("AMOUNT_EXCEEDS_24_MONTHS_OF_SALARY")
        ));

        var response = service.submit(applicationId, "owner@example.com", buildSubmitRequest(false, ""));

        assertEquals("SCORING_REJECTED", response.status());
        verify(offerRepository).deleteAllByApplicationId(applicationId);
        verify(createOffersService, never()).create(any(), any(), any(), any());
    }

    @Test
    void rejectsResubmissionForNonDraftStatus() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        ApplicationSubmitDataRepository submitDataRepository = mock(ApplicationSubmitDataRepository.class);
        ScoringSnapshotRepository scoringSnapshotRepository = mock(ScoringSnapshotRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        CreatePreliminaryOffersService createOffersService = mock(CreatePreliminaryOffersService.class);
        ScoringClient scoringClient = mock(ScoringClient.class);
        SubmitApplicationService service = new SubmitApplicationService(
                applicationAccessService,
                applicationRepository,
                submitDataRepository,
                scoringSnapshotRepository,
                offerRepository,
                createOffersService,
                scoringClient
        );
        UUID applicationId = UUID.randomUUID();

        when(applicationAccessService.getOwnedApplication(applicationId, "owner@example.com"))
                .thenReturn(buildApplication(applicationId, "SCORING_COMPLETED"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.submit(applicationId, "owner@example.com", buildSubmitRequest(false, ""))
        );

        assertEquals("Only draft applications can be submitted for scoring", exception.getMessage());
    }

    private ApplicationEntity buildApplication(UUID applicationId, String status) {
        OffsetDateTime now = OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC);
        return new ApplicationEntity(
                applicationId,
                status,
                new BigDecimal("500000.00"),
                12,
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "ivan@example.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "567890",
                now,
                now,
                "ANNUITY"
        );
    }

    private SubmitApplicationRequest buildSubmitRequest(boolean salaryClient, String accountNumber) {
        return new SubmitApplicationRequest(
                false,
                salaryClient,
                SubmitApplicationRequest.GenderType.MALE,
                SubmitApplicationRequest.MaritalStatusType.SINGLE,
                1,
                LocalDate.of(2015, 1, 1),
                "770-001",
                accountNumber,
                new SubmitApplicationRequest.Employment(
                        SubmitApplicationRequest.EmploymentStatusType.EMPLOYED,
                        "7701234567",
                        new BigDecimal("120000.00"),
                        SubmitApplicationRequest.PositionType.OTHER,
                        24,
                        12
                )
        );
    }

    private ScoringEvaluationResponse buildScoringResponse(String decision, List<String> reasons) {
        return new ScoringEvaluationResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                decision,
                new BigDecimal("780.00"),
                "B",
                "score-v1",
                "APPROVED".equals(decision) ? new BigDecimal("700000.00") : BigDecimal.ZERO,
                "APPROVED".equals(decision) ? 24 : null,
                "APPROVED".equals(decision) ? new BigDecimal("12.50") : null,
                reasons,
                OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC)
        );
    }
}
