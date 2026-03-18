package com.dpnevsky.creditcalculator.application.infrastructure.client.scoring;

import com.dpnevsky.creditcalculator.application.application.port.out.ScoringClient;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationRequest;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestScoringClient implements ScoringClient {

    private final RestClient restClient;

    public RestScoringClient(
            RestClient.Builder restClientBuilder,
            @Value("${integration.scoring.base-url}") String scoringBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(scoringBaseUrl)
                .build();
    }

    @Override
    public ScoringEvaluationResponse evaluate(ScoringEvaluationRequest request) {
        return restClient.post()
                .uri("/internal/scoring/evaluate")
                .body(request)
                .retrieve()
                .body(ScoringEvaluationResponse.class);
    }
}