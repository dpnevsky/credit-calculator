package com.dpnevsky.creditcalculator.scoring.domain.scoring;

import com.dpnevsky.creditcalculator.calculator.service.util.loancheck.AgeCheckHandler;
import com.dpnevsky.creditcalculator.calculator.service.util.loancheck.EmploymentPositionCheckHandler;
import com.dpnevsky.creditcalculator.calculator.service.util.loancheck.EmploymentStatusCheckHandler;
import com.dpnevsky.creditcalculator.calculator.service.util.loancheck.GenderCheckHandler;
import com.dpnevsky.creditcalculator.calculator.service.util.loancheck.InsuranceEnabledAndSalaryClientCheckHandler;
import com.dpnevsky.creditcalculator.calculator.service.util.loancheck.LoanApplicationProcessor;
import com.dpnevsky.creditcalculator.calculator.service.util.loancheck.LoanScoringData;
import com.dpnevsky.creditcalculator.calculator.service.util.loancheck.MaritalStatusCheckHandler;
import com.dpnevsky.creditcalculator.calculator.service.util.loancheck.SalaryAndAmountCheckHandler;
import com.dpnevsky.creditcalculator.calculator.service.util.loancheck.WorkExperienceCheckHandler;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class LegacyScoringPolicyService {

    private static final BigDecimal MIN_RATE = BigDecimal.ONE;

    @Value("${loan.base-rate:15.00}")
    private BigDecimal baseRate;

    private final LoanApplicationProcessor loanApplicationProcessor = new LoanApplicationProcessor(List.of(
            new AgeCheckHandler(),
            new EmploymentStatusCheckHandler(),
            new SalaryAndAmountCheckHandler(),
            new WorkExperienceCheckHandler(),
            new EmploymentPositionCheckHandler(),
            new GenderCheckHandler(),
            new InsuranceEnabledAndSalaryClientCheckHandler(),
            new MaritalStatusCheckHandler()
    ));

    public LegacyScoringDecision evaluate(ScoringEvaluationRequest request) {
        List<String> rejectionReasons = validateRequest(request);

        boolean approved = Boolean.TRUE.equals(request.prescoringSnapshot().prescorePassed())
                && rejectionReasons.isEmpty();

        BigDecimal calculatedRate = approved
                ? calculateRate(request)
                : BigDecimal.ZERO;

        BigDecimal scoreValue = calculateScoreValue(approved, calculatedRate);
        String riskGrade = calculateRiskGrade(approved, calculatedRate);
        String decision = approved ? "APPROVED" : "REJECTED";

        BigDecimal maxApprovedAmount = approved
                ? request.loanRequest().amount()
                : BigDecimal.ZERO;

        Integer maxTermMonths = approved
                ? request.loanRequest().termMonths()
                : null;

        BigDecimal finalRate = approved
                ? calculatedRate
                : null;

        return new LegacyScoringDecision(
                decision,
                scoreValue,
                riskGrade,
                finalRate,
                maxApprovedAmount,
                maxTermMonths,
                rejectionReasons
        );
    }

    private List<String> validateRequest(ScoringEvaluationRequest request) {
        List<String> reasons = new ArrayList<>();

        if (!Boolean.TRUE.equals(request.prescoringSnapshot().prescorePassed())) {
            reasons.add("PRESCORING_NOT_PASSED");
        }
        reasons.addAll(loanApplicationProcessor.processLoanApplication(toLoanScoringData(request), baseRate).rejectionReasons());

        return reasons;
    }

    private BigDecimal calculateRate(ScoringEvaluationRequest request) {
        BigDecimal rate = loanApplicationProcessor.processLoanApplication(toLoanScoringData(request), baseRate).rate();
        return rate.max(MIN_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateScoreValue(boolean approved, BigDecimal rate) {
        if (!approved) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal rawScore = BigDecimal.valueOf(900)
                .subtract(rate.multiply(BigDecimal.valueOf(20)));

        if (rawScore.compareTo(BigDecimal.valueOf(850)) > 0) {
            rawScore = BigDecimal.valueOf(850);
        }

        if (rawScore.compareTo(BigDecimal.valueOf(300)) < 0) {
            rawScore = BigDecimal.valueOf(300);
        }

        return rawScore.setScale(2, RoundingMode.HALF_UP);
    }

    private String calculateRiskGrade(boolean approved, BigDecimal rate) {
        if (!approved) {
            return "D";
        }

        if (rate.compareTo(BigDecimal.valueOf(10)) <= 0) {
            return "A";
        }
        if (rate.compareTo(BigDecimal.valueOf(15)) <= 0) {
            return "B";
        }
        if (rate.compareTo(BigDecimal.valueOf(20)) <= 0) {
            return "C";
        }
        return "D";
    }

    private LoanScoringData toLoanScoringData(ScoringEvaluationRequest request) {
        return new LoanScoringData(
                request.loanRequest().amount(),
                request.birthDate(),
                LoanScoringData.GenderType.valueOf(request.gender().name()),
                LoanScoringData.MaritalStatusType.valueOf(request.maritalStatus().name()),
                request.dependentAmount(),
                Boolean.TRUE.equals(request.loanRequest().insuranceEnabled()),
                Boolean.TRUE.equals(request.loanRequest().salaryClient()),
                new LoanScoringData.Employment(
                        LoanScoringData.EmploymentStatusType.valueOf(request.employment().employmentStatus().name()),
                        request.employment().salary(),
                        LoanScoringData.PositionType.valueOf(request.employment().position().name()),
                        request.employment().workExperienceTotal(),
                        request.employment().workExperienceCurrent()
                )
        );
    }

    public record LegacyScoringDecision(
            String decision,
            BigDecimal scoreValue,
            String riskGrade,
            BigDecimal finalRate,
            BigDecimal maxApprovedAmount,
            Integer maxTermMonths,
            List<String> rejectionReasons
    ) {
    }
}
