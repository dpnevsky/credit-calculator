package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.SubmitApplicationRequest;
import com.dpnevsky.creditcalculator.application.domain.offers.LegacyPreOfferGenerationService;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.OfferEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationSubmitDataRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.OfferRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ScoringSnapshotRepository;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationRequest;
import com.dpnevsky.creditcalculator.scoring.application.service.ScoringEvaluationService;
import com.dpnevsky.creditcalculator.scoring.domain.scoring.LegacyScoringPolicyService;
import com.dpnevsky.creditcalculator.scoring.infrastructure.persistence.entity.ScoringRequestEntity;
import com.dpnevsky.creditcalculator.scoring.infrastructure.persistence.entity.ScoringResultEntity;
import com.dpnevsky.creditcalculator.scoring.infrastructure.persistence.repository.ScoringRequestRepository;
import com.dpnevsky.creditcalculator.scoring.infrastructure.persistence.repository.ScoringResultRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises real scoring, submission and offer generation with repository doubles.
 * It does not start HTTP, PostgreSQL, Kafka or a Spring transaction manager.
 */
class ScoringToOffersTest {

    @ParameterizedTest
    @CsvSource({
            "false, false, 660.00, B",
            "true,  false, 680.00, B",
            "false, true,  720.00, A",
            "true,  true,  740.00, A"
    })
    void submissionPricesAllOffersFromTheSameBaseRate(
            boolean insuranceEnabled,
            boolean salaryClient,
            String expectedScore,
            String expectedRiskGrade
    ) {
        Fixture fixture = new Fixture("15.00");

        var response = fixture.submitService.submit(
                fixture.applicationId,
                "owner@example.com",
                buildSubmitRequest(insuranceEnabled, salaryClient)
        );

        assertEquals("SCORING_COMPLETED", response.status());
        assertEquals("APPROVED", response.decision());
        assertEquals(new BigDecimal("12.00"), response.approvedRate());
        assertEquals(new BigDecimal(expectedScore), response.scoreValue());
        assertEquals(expectedRiskGrade, response.riskGrade());
        assertEquals(new BigDecimal("600000.00"), response.approvedAmount());
        assertEquals(24, response.approvedTermMonths());
        assertTrue(response.reasons().isEmpty());
        assertEquals(insuranceEnabled, fixture.scoringRequest.get().loanRequest().insuranceEnabled());
        assertEquals(salaryClient, fixture.scoringRequest.get().loanRequest().salaryClient());

        assertOffers(fixture.savedOffers, "12.00", "11.00", "9.00", "8.00");
        verify(fixture.snapshots).save(any());
        verify(fixture.scoringRequests, times(1)).save(any());
        verify(fixture.scoringResults, times(1)).save(any());
    }

    @ParameterizedTest
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    void repeatedScoringRequestKeepsTheBaseRateAndOfferPrices(
            boolean insuranceEnabled,
            boolean salaryClient
    ) {
        Fixture fixture = new Fixture("15.00");
        fixture.submitService.submit(
                fixture.applicationId,
                "owner@example.com",
                buildSubmitRequest(insuranceEnabled, salaryClient)
        );

        var originalRequest = fixture.scoringRequest.get();
        var repeated = fixture.scoringService.evaluate(originalRequest);
        var repeatedAgain = fixture.scoringService.evaluate(originalRequest);

        assertEquals(repeated, repeatedAgain);
        assertEquals(new BigDecimal("12.00"), repeated.baseInterestRate());
        assertEquals("APPROVED", repeated.decision());
        assertEquals(
                fixture.resultsByRequest.values().iterator().next().getScoreValue(),
                repeated.scoreValue()
        );
        fixture.savedOffers.clear();
        fixture.createOffersService.create(
                fixture.applicationId,
                repeated.maxApprovedAmount(),
                repeated.maxTermMonths(),
                repeated.baseInterestRate()
        );
        assertOffers(fixture.savedOffers, "12.00", "11.00", "9.00", "8.00");
        verify(fixture.scoringRequests, times(1)).save(any());
        verify(fixture.scoringResults, times(1)).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "false, false, 5.00, 2.00, 1.00, 1.00, 1.00",
            "true,  false, 5.00, 2.00, 1.00, 1.00, 1.00",
            "false, true,  5.00, 2.00, 1.00, 1.00, 1.00",
            "true,  true,  5.00, 2.00, 1.00, 1.00, 1.00",
            "true,  true,  4.50, 1.50, 1.00, 1.00, 1.00",
            "true,  true,  3.00, 1.00, 1.00, 1.00, 1.00"
    })
    void submissionRetainsTheMinimumRateWithoutReversingClampedDiscounts(
            boolean insuranceEnabled,
            boolean salaryClient,
            String configuredRate,
            String plainRate,
            String insuranceRate,
            String salaryRate,
            String bothRate
    ) {
        Fixture fixture = new Fixture(configuredRate);
        var response = fixture.submitService.submit(
                fixture.applicationId,
                "owner@example.com",
                buildSubmitRequest(insuranceEnabled, salaryClient)
        );

        assertEquals(new BigDecimal(plainRate), response.approvedRate());
        assertOffers(fixture.savedOffers, plainRate, insuranceRate, salaryRate, bothRate);
    }

    private void assertOffers(
            List<OfferEntity> offers,
            String plainRate,
            String insuranceRate,
            String salaryRate,
            String bothRate
    ) {
        assertEquals(4, offers.size());
        assertOffer(offers, false, false, plainRate, "600000.00");
        assertOffer(offers, true, false, insuranceRate, "624000.00");
        assertOffer(offers, false, true, salaryRate, "600000.00");
        assertOffer(offers, true, true, bothRate, "624000.00");
    }

    private void assertOffer(
            List<OfferEntity> offers,
            boolean insuranceEnabled,
            boolean salaryClient,
            String expectedRate,
            String expectedTotal
    ) {
        List<OfferEntity> matching = offers.stream()
                .filter(offer -> offer.getInsuranceEnabled() == insuranceEnabled
                        && offer.getSalaryClient() == salaryClient)
                .toList();
        assertEquals(1, matching.size());
        OfferEntity offer = matching.get(0);
        assertEquals(new BigDecimal(expectedRate), offer.getRate());
        assertEquals(new BigDecimal("600000.00"), offer.getRequestedAmount());
        assertEquals(new BigDecimal(expectedTotal), offer.getTotalAmount());
        assertEquals(24, offer.getTermMonths());

        // Independent annuity expectation: pricing must flow through to payments.
        double monthlyRate = new BigDecimal(expectedRate).doubleValue() / 1200;
        double factor = Math.pow(1 + monthlyRate, 24);
        BigDecimal expectedPayment = BigDecimal.valueOf(
                new BigDecimal(expectedTotal).doubleValue() * monthlyRate * factor / (factor - 1)
        ).setScale(0, RoundingMode.HALF_EVEN);
        assertEquals(0, expectedPayment.compareTo(offer.getMonthlyPayment()));
        assertTrue(offer.getRate().compareTo(BigDecimal.ONE) >= 0);
    }

    private SubmitApplicationRequest buildSubmitRequest(boolean insuranceEnabled, boolean salaryClient) {
        return new SubmitApplicationRequest(
                insuranceEnabled,
                salaryClient,
                SubmitApplicationRequest.GenderType.MALE,
                SubmitApplicationRequest.MaritalStatusType.SINGLE,
                1,
                LocalDate.now().minusYears(10),
                "770-001",
                salaryClient ? "40817810000000000001" : null,
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

    private static class Fixture {
        private final UUID applicationId = UUID.randomUUID();
        private final ScoringRequestRepository scoringRequests = mock(ScoringRequestRepository.class);
        private final ScoringResultRepository scoringResults = mock(ScoringResultRepository.class);
        private final ScoringSnapshotRepository snapshots = mock(ScoringSnapshotRepository.class);
        private final Map<UUID, ScoringRequestEntity> requestsById = new HashMap<>();
        private final Map<UUID, ScoringResultEntity> resultsByRequest = new HashMap<>();
        private final AtomicReference<ScoringEvaluationRequest> scoringRequest = new AtomicReference<>();
        private final List<OfferEntity> savedOffers = new ArrayList<>();
        private final ScoringEvaluationService scoringService;
        private final CreatePreliminaryOffersService createOffersService;
        private final SubmitApplicationService submitService;

        private Fixture(String configuredRate) {
            LegacyScoringPolicyService policy = new LegacyScoringPolicyService();
            ReflectionTestUtils.setField(policy, "baseRate", new BigDecimal(configuredRate));
            scoringService = new ScoringEvaluationService(scoringRequests, scoringResults, policy);
            when(scoringRequests.findByRequestId(any())).thenAnswer(
                    invocation -> Optional.ofNullable(requestsById.get(invocation.getArgument(0)))
            );
            when(scoringRequests.save(any())).thenAnswer(invocation -> {
                ScoringRequestEntity entity = invocation.getArgument(0);
                requestsById.put(entity.getRequestId(), entity);
                return entity;
            });
            when(scoringResults.findByScoringRequestId(any())).thenAnswer(invocation -> {
                ScoringResultEntity entity = resultsByRequest.get(invocation.getArgument(0));
                return entity == null ? List.of() : List.of(entity);
            });
            when(scoringResults.save(any())).thenAnswer(invocation -> {
                ScoringResultEntity entity = invocation.getArgument(0);
                resultsByRequest.put(entity.getScoringRequestId(), entity);
                return entity;
            });

            OfferRepository offers = mock(OfferRepository.class);
            when(offers.save(any())).thenAnswer(invocation -> {
                OfferEntity entity = invocation.getArgument(0);
                savedOffers.add(entity);
                return entity;
            });
            createOffersService = new CreatePreliminaryOffersService(new LegacyPreOfferGenerationService(), offers);
            ApplicationAccessService access = mock(ApplicationAccessService.class);
            OffsetDateTime now = OffsetDateTime.now();
            when(access.getOwnedApplicationForUpdate(applicationId, "owner@example.com")).thenReturn(
                    new ApplicationEntity(
                            applicationId, "DRAFT", new BigDecimal("600000.00"), 24,
                            "Ivan", "Ivanov", "Ivanovich", "owner@example.com",
                            LocalDate.now().minusYears(36), "1234", "567890", now, now, "ANNUITY"
                    )
            );
            submitService = new SubmitApplicationService(
                    access,
                    mock(ApplicationRepository.class),
                    mock(ApplicationSubmitDataRepository.class),
                    snapshots,
                    offers,
                    createOffersService,
                    request -> {
                        scoringRequest.set(request);
                        return scoringService.evaluate(request);
                    }
            );
        }
    }
}
