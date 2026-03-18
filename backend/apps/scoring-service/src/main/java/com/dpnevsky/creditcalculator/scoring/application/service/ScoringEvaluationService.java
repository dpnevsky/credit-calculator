package com.dpnevsky.creditcalculator.scoring.application.service;

import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationRequest;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ScoringEvaluationService {

    public ScoringEvaluationResponse evaluate(ScoringEvaluationRequest request) {
        BigDecimal income = request.applicant().monthlyIncome();
        BigDecimal expenses = request.applicant().monthlyExpenses();
        BigDecimal existingDebt = request.applicant().existingDebt();
        BigDecimal requestedAmount = request.loanRequest().amount();

        BigDecimal freeCashFlow = income
                .subtract(expenses)
                .subtract(existingDebt);

        boolean approved = Boolean.TRUE.equals(request.prescoring().prescorePassed())
                && freeCashFlow.compareTo(BigDecimal.ZERO) > 0;

        BigDecimal scoreValue = calculateScore(freeCashFlow, requestedAmount);
        String riskGrade = calculateRiskGrade(scoreValue);
        String decision = approved ? "APPROVED" : "REJECTED";

        BigDecimal maxApprovedAmount = approved
                ? requestedAmount.min(freeCashFlow.multiply(BigDecimal.valueOf(12)))
                : BigDecimal.ZERO;

        Integer maxTermMonths = approved ? request.loanRequest().termMonths() : null;

        BigDecimal baseInterestRate = approved
                ? calculateBaseInterestRate(riskGrade)
                : null;

        List<String> reasons = approved
                ? List.of()
                : List.of("INSUFFICIENT_FREE_CASH_FLOW");

        return new ScoringEvaluationResponse(
                request.requestId(),
                request.applicationId(),
                decision,
                scoreValue,
                riskGrade,
                "score-v1",
                maxApprovedAmount,
                maxTermMonths,
                baseInterestRate,
                reasons,
                OffsetDateTime.now()
        );
    }

    private BigDecimal calculateScore(BigDecimal freeCashFlow, BigDecimal requestedAmount) {
        if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal ratio = freeCashFlow
                .max(BigDecimal.ZERO)
                .divide(requestedAmount, 4, RoundingMode.HALF_UP);

        BigDecimal rawScore = BigDecimal.valueOf(600)
                .add(ratio.multiply(BigDecimal.valueOf(1000)));

        if (rawScore.compareTo(BigDecimal.valueOf(850)) > 0) {
            return BigDecimal.valueOf(850);
        }

        return rawScore.setScale(2, RoundingMode.HALF_UP);
    }

    private String calculateRiskGrade(BigDecimal scoreValue) {
        if (scoreValue.compareTo(BigDecimal.valueOf(800)) >= 0) {
            return "A";
        }
        if (scoreValue.compareTo(BigDecimal.valueOf(700)) >= 0) {
            return "B";
        }
        if (scoreValue.compareTo(BigDecimal.valueOf(650)) >= 0) {
            return "C";
        }
        return "D";
    }

    private BigDecimal calculateBaseInterestRate(String riskGrade) {
        return switch (riskGrade) {
            case "A" -> BigDecimal.valueOf(11.90);
            case "B" -> BigDecimal.valueOf(13.50);
            case "C" -> BigDecimal.valueOf(16.90);
            default -> BigDecimal.valueOf(21.90);
        };
    }
}