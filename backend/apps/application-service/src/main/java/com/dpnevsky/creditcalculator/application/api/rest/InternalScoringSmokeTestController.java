package com.dpnevsky.creditcalculator.application.api.rest;

import com.dpnevsky.creditcalculator.application.application.service.ScoringSmokeTestService;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/test/scoring")
public class InternalScoringSmokeTestController {

    private final ScoringSmokeTestService scoringSmokeTestService;

    public InternalScoringSmokeTestController(ScoringSmokeTestService scoringSmokeTestService) {
        this.scoringSmokeTestService = scoringSmokeTestService;
    }

    @PostMapping("/evaluate-sample")
    public ResponseEntity<ScoringEvaluationResponse> evaluateSample() {
        return ResponseEntity.ok(scoringSmokeTestService.evaluateSample());
    }
}