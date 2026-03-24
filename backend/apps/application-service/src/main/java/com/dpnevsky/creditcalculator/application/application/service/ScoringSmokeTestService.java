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
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CREDIT_STANDARD",
                OffsetDateTime.now(),

                "Danil",
                "Pnevsky",
                "Igorevich",

                ScoringEvaluationRequest.GenderType.MALE,
                LocalDate.of(1995, 1, 10),

                "1234",
                "567890",
                LocalDate.of(2015, 6, 10),
                "UFMS TEST",

                ScoringEvaluationRequest.MaritalStatusType.MARRIED,
                0,

                new ScoringEvaluationRequest.Employment(
                        ScoringEvaluationRequest.EmploymentStatusType.EMPLOYED,
                        "7701234567",
                        BigDecimal.valueOf(120000.00),
                        ScoringEvaluationRequest.PositionType.MID_MANAGER,
                        60,
                        24
                ),

                "40702810900000000001",

                new ScoringEvaluationRequest.LoanRequest(
                        BigDecimal.valueOf(300000.00),
                        24,
                        "RUB",
                        true,
                        true
                ),

                new ScoringEvaluationRequest.PrescoringSnapshot(
                        true,
                        "prescore-v1"
                )
        );

        return scoringClient.evaluate(request);
    }
}