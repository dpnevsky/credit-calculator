package com.dpnevsky.creditcalculator.scoring.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scoring_results")
public class ScoringResultEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "scoring_request_id", nullable = false)
    private UUID scoringRequestId;

    @Column(name = "decision", nullable = false, length = 32)
    private String decision;

    @Column(name = "score_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal scoreValue;

    @Column(name = "risk_grade", nullable = false, length = 16)
    private String riskGrade;

    @Column(name = "rules_version", nullable = false, length = 64)
    private String rulesVersion;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    protected ScoringResultEntity() {
        // for JPA
    }

    public ScoringResultEntity(
            UUID id,
            UUID scoringRequestId,
            String decision,
            BigDecimal scoreValue,
            String riskGrade,
            String rulesVersion,
            OffsetDateTime calculatedAt
    ) {
        this.id = id;
        this.scoringRequestId = scoringRequestId;
        this.decision = decision;
        this.scoreValue = scoreValue;
        this.riskGrade = riskGrade;
        this.rulesVersion = rulesVersion;
        this.calculatedAt = calculatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getScoringRequestId() {
        return scoringRequestId;
    }

    public String getDecision() {
        return decision;
    }

    public BigDecimal getScoreValue() {
        return scoreValue;
    }

    public String getRiskGrade() {
        return riskGrade;
    }

    public String getRulesVersion() {
        return rulesVersion;
    }

    public OffsetDateTime getCalculatedAt() {
        return calculatedAt;
    }
}