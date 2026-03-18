package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.application.port.out.ScoringClient;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationRequest;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ScoringSmokeTestService {

    private final ScoringClient scoringClient;

    public ScoringSmokeTestService(ScoringClient scoringClient) {
        this.scoringClient = scoringClient;
    }

    public ScoringEvaluationResponse evaluateSample() {
        ScoringEvaluationRequest request = new ScoringEvaluationRequest(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "CREDIT_STANDARD",
                OffsetDateTime.now(),
                new ScoringEvaluationRequest.Applicant(
                        LocalDate.of(1995, 1, 10),
                        "EMPLOYED",
                        BigDecimal.valueOf(120000.00),
                        BigDecimal.valueOf(45000.00),
                        BigDecimal.valueOf(12000.00)
                ),
                new ScoringEvaluationRequest.LoanRequest(
                        BigDecimal.valueOf(100000.00),
                        24,
                        "RUB"
                ),
                new ScoringEvaluationRequest.PrescoringSnapshot(
                        true,
                        true,
                        BigDecimal.valueOf(0.10),
                        true
                )
        );

        return scoringClient.evaluate(request);
    }
}