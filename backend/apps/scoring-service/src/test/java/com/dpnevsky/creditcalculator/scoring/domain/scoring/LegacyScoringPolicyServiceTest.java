package com.dpnevsky.creditcalculator.scoring.domain.scoring;

import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    }

    @ParameterizedTest
    @CsvSource({
            "false, false, 12.00, 660.00, B",
            "true,  false, 11.00, 680.00, B",
            "false, true,   9.00, 720.00, A",
            "true,  true,   8.00, 740.00, A"
    })
    void separatesOfferBaseRateWithoutChangingLegacyRisk(
            boolean insuranceEnabled,
            boolean salaryClient,
            String expectedFinalRate,
            String expectedScore,
            String expectedRiskGrade
    ) {
        var decision = buildService().evaluate(buildRequest(
                "600000.00", "120000.00", 1, insuranceEnabled, salaryClient,
                ScoringEvaluationRequest.EmploymentStatusType.EMPLOYED,
                ScoringEvaluationRequest.PositionType.OTHER,
                ScoringEvaluationRequest.MaritalStatusType.SINGLE
        ));

        assertEquals("APPROVED", decision.decision());
        assertEquals(new BigDecimal("12.00"), decision.baseInterestRate());
        assertEquals(new BigDecimal(expectedFinalRate), decision.finalRate());
        assertEquals(new BigDecimal(expectedScore), decision.scoreValue());
        assertEquals(expectedRiskGrade, decision.riskGrade());
        assertEquals(new BigDecimal("600000.00"), decision.maxApprovedAmount());
        assertEquals(24, decision.maxTermMonths());
        assertTrue(decision.rejectionReasons().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "SELF_EMPLOYED, OTHER, DIVORCED, 15.00, 11.00",
            "BUSINESS_OWNER, MID_MANAGER, MARRIED, 8.00, 4.00",
            "EMPLOYED, TOP_MANAGER, MARRIED, 6.00, 2.00"
    })
    void preservesEmploymentPositionGenderAndMaritalAdjustments(
            ScoringEvaluationRequest.EmploymentStatusType employmentStatus,
            ScoringEvaluationRequest.PositionType position,
            ScoringEvaluationRequest.MaritalStatusType maritalStatus,
            String expectedBaseRate,
            String expectedFinalRate
    ) {
        var decision = buildService().evaluate(buildRequest(
                "600000.00", "120000.00", 1, true, true,
                employmentStatus, position, maritalStatus
        ));

        assertEquals("APPROVED", decision.decision());
        assertEquals(new BigDecimal(expectedBaseRate), decision.baseInterestRate());
        assertEquals(new BigDecimal(expectedFinalRate), decision.finalRate());
    }

    @ParameterizedTest
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    void productOptionsDoNotChangeIncomeRejection(boolean insuranceEnabled, boolean salaryClient) {
        var decision = buildService().evaluate(buildRequest(
                "600000.00", "50000.00", 2, insuranceEnabled, salaryClient,
                ScoringEvaluationRequest.EmploymentStatusType.EMPLOYED,
                ScoringEvaluationRequest.PositionType.OTHER,
                ScoringEvaluationRequest.MaritalStatusType.SINGLE
        ));

        assertEquals("REJECTED", decision.decision());
        assertEquals(java.util.List.of("AMOUNT_EXCEEDS_24_MONTHS_OF_SALARY"), decision.rejectionReasons());
        assertEquals(BigDecimal.ZERO, decision.maxApprovedAmount());
        assertNull(decision.maxTermMonths());
        assertNull(decision.baseInterestRate());
        assertNull(decision.finalRate());
        assertEquals(new BigDecimal("0.00"), decision.scoreValue());
        assertEquals("D", decision.riskGrade());
    }

    @Test
    void computesBaseBeforeDiscountsInsteadOfAddingThemBackToTheClampedRate() {
        LegacyScoringPolicyService service = buildService();
        ReflectionTestUtils.setField(service, "baseRate", new BigDecimal("5.00"));
        var decision = service.evaluate(buildRequest(
                "600000.00", "120000.00", 1, true, true,
                ScoringEvaluationRequest.EmploymentStatusType.EMPLOYED,
                ScoringEvaluationRequest.PositionType.OTHER,
                ScoringEvaluationRequest.MaritalStatusType.SINGLE
        ));

        assertEquals(new BigDecimal("2.00"), decision.baseInterestRate());
        assertEquals(new BigDecimal("1.00"), decision.finalRate());
        assertEquals(new BigDecimal("850.00"), decision.scoreValue());
        assertEquals("A", decision.riskGrade());
    }

    private LegacyScoringPolicyService buildService() {
        LegacyScoringPolicyService service = new LegacyScoringPolicyService();
        ReflectionTestUtils.setField(service, "baseRate", new BigDecimal("15.00"));
        return service;
    }

    private ScoringEvaluationRequest buildRequest(String amount, String salary, int dependentAmount) {
        return buildRequest(
                amount, salary, dependentAmount, false, false,
                ScoringEvaluationRequest.EmploymentStatusType.EMPLOYED,
                ScoringEvaluationRequest.PositionType.OTHER,
                ScoringEvaluationRequest.MaritalStatusType.SINGLE
        );
    }

    private ScoringEvaluationRequest buildRequest(
            String amount,
            String salary,
            int dependentAmount,
            boolean insuranceEnabled,
            boolean salaryClient,
            ScoringEvaluationRequest.EmploymentStatusType employmentStatus,
            ScoringEvaluationRequest.PositionType position,
            ScoringEvaluationRequest.MaritalStatusType maritalStatus
    ) {
        return new ScoringEvaluationRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CREDIT_STANDARD",
                OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC),
                "Ivan",
                "Ivanov",
                "Ivanovich",
                ScoringEvaluationRequest.GenderType.MALE,
                LocalDate.now().minusYears(36),
                "1234",
                "567890",
                LocalDate.of(2015, 1, 1),
                "770-001",
                maritalStatus,
                dependentAmount,
                new ScoringEvaluationRequest.Employment(
                        employmentStatus,
                        "7701234567",
                        new BigDecimal(salary),
                        position,
                        24,
                        12
                ),
                salaryClient ? "40817810000000000001" : null,
                new ScoringEvaluationRequest.LoanRequest(
                        new BigDecimal(amount),
                        24,
                        "RUB",
                        insuranceEnabled,
                        salaryClient
                ),
                new ScoringEvaluationRequest.PrescoringSnapshot(
                        true,
                        "prescore-v1"
                )
        );
    }
}
