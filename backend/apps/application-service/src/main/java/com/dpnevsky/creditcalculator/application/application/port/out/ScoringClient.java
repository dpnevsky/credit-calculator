package com.dpnevsky.creditcalculator.application.application.port.out;

import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationRequest;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationResponse;

public interface ScoringClient {

    ScoringEvaluationResponse evaluate(ScoringEvaluationRequest request);
}