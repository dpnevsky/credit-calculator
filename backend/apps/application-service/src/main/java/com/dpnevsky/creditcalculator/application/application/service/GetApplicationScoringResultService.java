package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationScoringResultResponse;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ScoringSnapshotEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ScoringSnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetApplicationScoringResultService {

    private final ApplicationAccessService applicationAccessService;
    private final ScoringSnapshotRepository scoringSnapshotRepository;

    public GetApplicationScoringResultService(
            ApplicationAccessService applicationAccessService,
            ScoringSnapshotRepository scoringSnapshotRepository
    ) {
        this.applicationAccessService = applicationAccessService;
        this.scoringSnapshotRepository = scoringSnapshotRepository;
    }

    public GetApplicationScoringResultResponse getLatestByApplicationId(UUID applicationId, String userEmail) {
        applicationAccessService.getOwnedApplication(applicationId, userEmail);

        ScoringSnapshotEntity scoringSnapshot = scoringSnapshotRepository
                .findTopByApplicationIdOrderByScoredAtDesc(applicationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Scoring result not found for application: " + applicationId
                ));

        return new GetApplicationScoringResultResponse(
                scoringSnapshot.getApplicationId(),
                scoringSnapshot.getDecision(),
                scoringSnapshot.getScoreValue(),
                scoringSnapshot.getRiskGrade(),
                scoringSnapshot.getRulesVersion(),
                scoringSnapshot.getApprovedAmount(),
                scoringSnapshot.getApprovedTermMonths(),
                scoringSnapshot.getApprovedRate(),
                parseReasons(scoringSnapshot.getReasonsJson()),
                scoringSnapshot.getScoredAt()
        );
    }

    private List<String> parseReasons(List<String> reasonsJson) {
        return reasonsJson == null ? List.of() : reasonsJson;
    }
}
