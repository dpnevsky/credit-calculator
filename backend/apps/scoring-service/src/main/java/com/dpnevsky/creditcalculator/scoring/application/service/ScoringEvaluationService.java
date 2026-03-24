package com.dpnevsky.creditcalculator.scoring.application.service;

import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationRequest;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationResponse;
import com.dpnevsky.creditcalculator.scoring.domain.scoring.LegacyScoringPolicyService;
import com.dpnevsky.creditcalculator.scoring.infrastructure.persistence.entity.ScoringRequestEntity;
import com.dpnevsky.creditcalculator.scoring.infrastructure.persistence.entity.ScoringResultEntity;
import com.dpnevsky.creditcalculator.scoring.infrastructure.persistence.repository.ScoringRequestRepository;
import com.dpnevsky.creditcalculator.scoring.infrastructure.persistence.repository.ScoringResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ScoringEvaluationService {

    private final ScoringRequestRepository scoringRequestRepository;
    private final ScoringResultRepository scoringResultRepository;
    private final LegacyScoringPolicyService legacyScoringPolicyService;

    public ScoringEvaluationService(
            ScoringRequestRepository scoringRequestRepository,
            ScoringResultRepository scoringResultRepository,
            LegacyScoringPolicyService legacyScoringPolicyService
    ) {
        this.scoringRequestRepository = scoringRequestRepository;
        this.scoringResultRepository = scoringResultRepository;
        this.legacyScoringPolicyService = legacyScoringPolicyService;
    }

    @Transactional
    public ScoringEvaluationResponse evaluate(ScoringEvaluationRequest request) {
        return scoringRequestRepository.findByRequestId(request.requestId())
                .map(existingRequest -> buildResponseForExistingRequest(existingRequest, request))
                .orElseGet(() -> evaluateAndPersistNewRequest(request));
    }

    private ScoringEvaluationResponse buildResponseForExistingRequest(
            ScoringRequestEntity existingRequest,
            ScoringEvaluationRequest request
    ) {
        List<ScoringResultEntity> existingResults =
                scoringResultRepository.findByScoringRequestId(existingRequest.getId());

        if (existingResults.isEmpty()) {
            throw new IllegalStateException(
                    "Scoring request exists, but scoring result was not found for requestId=" + request.requestId()
            );
        }

        ScoringResultEntity existingResult = existingResults.get(0);

        LegacyScoringPolicyService.LegacyScoringDecision policyDecision =
                legacyScoringPolicyService.evaluate(request);

        return new ScoringEvaluationResponse(
                request.requestId(),
                request.applicationId(),
                existingResult.getDecision(),
                existingResult.getScoreValue(),
                existingResult.getRiskGrade(),
                existingResult.getRulesVersion(),
                "APPROVED".equals(existingResult.getDecision()) ? policyDecision.maxApprovedAmount() : existingResultDecisionZeroAmount(),
                "APPROVED".equals(existingResult.getDecision()) ? policyDecision.maxTermMonths() : null,
                "APPROVED".equals(existingResult.getDecision()) ? policyDecision.finalRate() : null,
                "APPROVED".equals(existingResult.getDecision()) ? List.of() : policyDecision.rejectionReasons(),
                existingResult.getCalculatedAt()
        );
    }

    private ScoringEvaluationResponse evaluateAndPersistNewRequest(ScoringEvaluationRequest request) {
        OffsetDateTime now = OffsetDateTime.now();

        LegacyScoringPolicyService.LegacyScoringDecision decision =
                legacyScoringPolicyService.evaluate(request);

        ScoringRequestEntity scoringRequestEntity = new ScoringRequestEntity(
                UUID.randomUUID(),
                request.requestId(),
                request.applicationId(),
                request.productCode(),
                "APPROVED".equals(decision.decision()) ? "PROCESSED" : "REJECTED",
                now
        );
        scoringRequestRepository.save(scoringRequestEntity);

        ScoringResultEntity scoringResultEntity = new ScoringResultEntity(
                UUID.randomUUID(),
                scoringRequestEntity.getId(),
                decision.decision(),
                decision.scoreValue(),
                decision.riskGrade(),
                "score-v1",
                now
        );
        scoringResultRepository.save(scoringResultEntity);

        return new ScoringEvaluationResponse(
                request.requestId(),
                request.applicationId(),
                decision.decision(),
                decision.scoreValue(),
                decision.riskGrade(),
                "score-v1",
                decision.maxApprovedAmount(),
                decision.maxTermMonths(),
                decision.finalRate(),
                decision.rejectionReasons(),
                now
        );
    }

    private java.math.BigDecimal existingResultDecisionZeroAmount() {
        return java.math.BigDecimal.ZERO;
    }
}