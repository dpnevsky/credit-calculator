package com.dpnevsky.creditcalculator.scoring.domain.scoring;

import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Service
public class LegacyScoringPolicyService {

    private static final BigDecimal MIN_RATE = BigDecimal.ONE;
    private static final BigDecimal SELF_EMPLOYED_RATE_INCREASE = BigDecimal.valueOf(2);
    private static final BigDecimal BUSINESS_OWNER_RATE_INCREASE = BigDecimal.ONE;
    private static final BigDecimal MID_MANAGER_RATE_DISCOUNT = BigDecimal.valueOf(2);
    private static final BigDecimal TOP_MANAGER_RATE_DISCOUNT = BigDecimal.valueOf(3);
    private static final BigDecimal GENDER_DISCOUNT = BigDecimal.valueOf(3);
    private static final BigDecimal NON_BINARY_RATE_INCREASE = BigDecimal.valueOf(7);
    private static final BigDecimal INSURANCE_DISCOUNT = BigDecimal.ONE;
    private static final BigDecimal SALARY_CLIENT_DISCOUNT = BigDecimal.valueOf(3);
    private static final BigDecimal MARRIED_DISCOUNT = BigDecimal.valueOf(3);
    private static final BigDecimal DIVORCED_RATE_INCREASE = BigDecimal.ONE;
    private static final int MIN_AGE = 20;
    private static final int MAX_AGE = 65;
    private static final int MIN_TOTAL_EXPERIENCE_MONTHS = 18;
    private static final int MIN_CURRENT_EXPERIENCE_MONTHS = 3;
    private static final BigDecimal MAX_AMOUNT_TO_SALARY_MULTIPLIER = BigDecimal.valueOf(24);

    @Value("${loan.base-rate:15.00}")
    private BigDecimal baseRate;

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

        int age = calculateAge(request.birthDate(), LocalDate.now());
        if (age < MIN_AGE || age > MAX_AGE) {
            reasons.add("AGE_OUT_OF_RANGE");
        }

        ScoringEvaluationRequest.EmploymentStatusType employmentStatus = request.employment().employmentStatus();
        if (employmentStatus == ScoringEvaluationRequest.EmploymentStatusType.UNEMPLOYED) {
            reasons.add("UNEMPLOYED");
        }

        BigDecimal salary = request.employment().salary();
        BigDecimal requestedAmount = request.loanRequest().amount();
        if (requestedAmount.compareTo(salary.multiply(MAX_AMOUNT_TO_SALARY_MULTIPLIER)) > 0) {
            reasons.add("AMOUNT_EXCEEDS_24_MONTHS_OF_SALARY");
        }

        if (request.employment().workExperienceTotal() < MIN_TOTAL_EXPERIENCE_MONTHS) {
            reasons.add("TOTAL_WORK_EXPERIENCE_TOO_LOW");
        }

        if (request.employment().workExperienceCurrent() < MIN_CURRENT_EXPERIENCE_MONTHS) {
            reasons.add("CURRENT_WORK_EXPERIENCE_TOO_LOW");
        }

        return reasons;
    }

    private BigDecimal calculateRate(ScoringEvaluationRequest request) {
        BigDecimal rate = baseRate;

        switch (request.employment().employmentStatus()) {
            case SELF_EMPLOYED -> rate = rate.add(SELF_EMPLOYED_RATE_INCREASE);
            case BUSINESS_OWNER -> rate = rate.add(BUSINESS_OWNER_RATE_INCREASE);
            default -> {
            }
        }

        switch (request.employment().position()) {
            case MID_MANAGER -> rate = rate.subtract(MID_MANAGER_RATE_DISCOUNT);
            case TOP_MANAGER -> rate = rate.subtract(TOP_MANAGER_RATE_DISCOUNT);
            default -> {
            }
        }

        int age = calculateAge(request.birthDate(), LocalDate.now());
        switch (request.gender()) {
            case FEMALE -> {
                if (age >= 32 && age <= 60) {
                    rate = rate.subtract(GENDER_DISCOUNT);
                }
            }
            case MALE -> {
                if (age >= 30 && age <= 55) {
                    rate = rate.subtract(GENDER_DISCOUNT);
                }
            }
            case NON_BINARY -> rate = rate.add(NON_BINARY_RATE_INCREASE);
        }

        if (Boolean.TRUE.equals(request.loanRequest().insuranceEnabled())) {
            rate = rate.subtract(INSURANCE_DISCOUNT);
        }

        if (Boolean.TRUE.equals(request.loanRequest().salaryClient())) {
            rate = rate.subtract(SALARY_CLIENT_DISCOUNT);
        }

        switch (request.maritalStatus()) {
            case MARRIED -> rate = rate.subtract(MARRIED_DISCOUNT);
            case DIVORCED -> rate = rate.add(DIVORCED_RATE_INCREASE);
            default -> {
            }
        }

        if (rate.compareTo(MIN_RATE) < 0) {
            rate = MIN_RATE;
        }

        return rate.setScale(2, RoundingMode.HALF_UP);
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

    private int calculateAge(LocalDate birthDate, LocalDate currentDate) {
        return Period.between(birthDate, currentDate).getYears();
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