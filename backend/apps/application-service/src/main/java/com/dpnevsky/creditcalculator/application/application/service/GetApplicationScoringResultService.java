package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationScoringResultResponse;
import com.dpnevsky.creditcalculator.application.application.exception.ApplicationNotFoundException;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ScoringSnapshotEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ScoringSnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetApplicationScoringResultService {

    private final ApplicationRepository applicationRepository;
    private final ScoringSnapshotRepository scoringSnapshotRepository;

    public GetApplicationScoringResultService(
            ApplicationRepository applicationRepository,
            ScoringSnapshotRepository scoringSnapshotRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.scoringSnapshotRepository = scoringSnapshotRepository;
    }

    public GetApplicationScoringResultResponse getLatestByApplicationId(UUID applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ApplicationNotFoundException(applicationId);
        }

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