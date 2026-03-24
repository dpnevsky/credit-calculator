package com.dpnevsky.creditcalculator.scoring.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scoring_requests")
public class ScoringRequestEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "product_code", nullable = false, length = 64)
    private String productCode;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ScoringRequestEntity() {
        // for JPA
    }

    public ScoringRequestEntity(
            UUID id,
            UUID requestId,
            UUID applicationId,
            String productCode,
            String status,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.requestId = requestId;
        this.applicationId = applicationId;
        this.productCode = productCode;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}