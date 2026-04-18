package com.dpnevsky.creditcalculator.scoring.domain.scoring;

import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyScoringPolicyServiceTest {

    @Test
    void rejectsWhenDependentsMakeIncomeInsufficient() {
        LegacyScoringPolicyService service = buildService();

        LegacyScoringPolicyService.LegacyScoringDecision decision = service.evaluate(buildRequest("600000.00", "50000.00", 2));

        assertEquals("REJECTED", decision.decision());
        assertTrue(decision.rejectionReasons().contains("AMOUNT_EXCEEDS_24_MONTHS_OF_SALARY"));
    }

    @Test
    void approvesWhenEffectiveIncomeIsEnough() {
        LegacyScoringPolicyService service = buildService();

        LegacyScoringPolicyService.LegacyScoringDecision decision = service.evaluate(buildRequest("600000.00", "120000.00", 1));

        assertEquals("APPROVED", decision.decision());
        assertTrue(decision.rejectionReasons().isEmpty());
        assertEquals(new BigDecimal("15.00"), decision.finalRate());
    }

    private LegacyScoringPolicyService buildService() {
        LegacyScoringPolicyService service = new LegacyScoringPolicyService();
        ReflectionTestUtils.setField(service, "baseRate", new BigDecimal("15.00"));
        return service;
    }

    private ScoringEvaluationRequest buildRequest(String amount, String salary, int dependentAmount) {
        return new ScoringEvaluationRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CREDIT_STANDARD",
                OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC),
                "Ivan",
                "Ivanov",
                "Ivanovich",
                ScoringEvaluationRequest.GenderType.MALE,
                LocalDate.of(1990, 1, 1),
                "1234",
                "567890",
                LocalDate.of(2015, 1, 1),
                "770-001",
                ScoringEvaluationRequest.MaritalStatusType.SINGLE,
                dependentAmount,
                new ScoringEvaluationRequest.Employment(
                        ScoringEvaluationRequest.EmploymentStatusType.EMPLOYED,
                        "7701234567",
                        new BigDecimal(salary),
                        ScoringEvaluationRequest.PositionType.OTHER,
                        24,
                        12
                ),
                null,
                new ScoringEvaluationRequest.LoanRequest(
                        new BigDecimal(amount),
                        24,
                        "RUB",
                        false,
                        false
                ),
                new ScoringEvaluationRequest.PrescoringSnapshot(
                        true,
                        "prescore-v1"
                )
        );
    }
}
