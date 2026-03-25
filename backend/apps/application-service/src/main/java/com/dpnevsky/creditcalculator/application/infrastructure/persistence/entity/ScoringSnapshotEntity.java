package com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scoring_snapshots")
public class ScoringSnapshotEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "decision", nullable = false, length = 32)
    private String decision;

    @Column(name = "score_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal scoreValue;

    @Column(name = "risk_grade", nullable = false, length = 16)
    private String riskGrade;

    @Column(name = "rules_version", nullable = false, length = 64)
    private String rulesVersion;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "approved_term_months")
    private Integer approvedTermMonths;

    @Column(name = "approved_rate", precision = 8, scale = 2)
    private BigDecimal approvedRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reasons_json", nullable = false, columnDefinition = "jsonb")
    private List<String> reasonsJson;

    @Column(name = "scored_at", nullable = false)
    private OffsetDateTime scoredAt;

    protected ScoringSnapshotEntity() {
        // for JPA
    }

    public ScoringSnapshotEntity(
            UUID id,
            UUID applicationId,
            String decision,
            BigDecimal scoreValue,
            String riskGrade,
            String rulesVersion,
            BigDecimal approvedAmount,
            Integer approvedTermMonths,
            BigDecimal approvedRate,
            List<String> reasonsJson,
            OffsetDateTime scoredAt
    ) {
        this.id = id;
        this.applicationId = applicationId;
        this.decision = decision;
        this.scoreValue = scoreValue;
        this.riskGrade = riskGrade;
        this.rulesVersion = rulesVersion;
        this.approvedAmount = approvedAmount;
        this.approvedTermMonths = approvedTermMonths;
        this.approvedRate = approvedRate;
        this.reasonsJson = reasonsJson;
        this.scoredAt = scoredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
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

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public Integer getApprovedTermMonths() {
        return approvedTermMonths;
    }

    public BigDecimal getApprovedRate() {
        return approvedRate;
    }

    public List<String> getReasonsJson() {
        return reasonsJson;
    }

    public OffsetDateTime getScoredAt() {
        return scoredAt;
    }
}