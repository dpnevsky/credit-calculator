package com.dpnevsky.creditcalculator.scoring.api.internal.rest;

import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationRequest;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationResponse;
import com.dpnevsky.creditcalculator.scoring.application.service.ScoringEvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/scoring")
public class ScoringController {

    private final ScoringEvaluationService scoringEvaluationService;

    public ScoringController(ScoringEvaluationService scoringEvaluationService) {
        this.scoringEvaluationService = scoringEvaluationService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<ScoringEvaluationResponse> evaluate(
            @Valid @RequestBody ScoringEvaluationRequest request
    ) {
        ScoringEvaluationResponse response = scoringEvaluationService.evaluate(request);
        return ResponseEntity.ok(response);
    }
}