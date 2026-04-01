package com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "offers")
public class OfferEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "monthly_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal rate;

    @Column(name = "insurance_enabled", nullable = false)
    private Boolean insuranceEnabled;

    @Column(name = "salary_client", nullable = false)
    private Boolean salaryClient;

    @Column(name = "selected", nullable = false)
    private Boolean selected;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected OfferEntity() {
    }

    public OfferEntity(
            UUID id,
            UUID applicationId,
            BigDecimal requestedAmount,
            BigDecimal totalAmount,
            Integer termMonths,
            BigDecimal monthlyPayment,
            BigDecimal rate,
            Boolean insuranceEnabled,
            Boolean salaryClient,
            Boolean selected,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.applicationId = applicationId;
        this.requestedAmount = requestedAmount;
        this.totalAmount = totalAmount;
        this.termMonths = termMonths;
        this.monthlyPayment = monthlyPayment;
        this.rate = rate;
        this.insuranceEnabled = insuranceEnabled;
        this.salaryClient = salaryClient;
        this.selected = selected;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getApplicationId() { return applicationId; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Integer getTermMonths() { return termMonths; }
    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public BigDecimal getRate() { return rate; }
    public Boolean getInsuranceEnabled() { return insuranceEnabled; }
    public Boolean getSalaryClient() { return salaryClient; }
    public Boolean getSelected() { return selected; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setSelected(Boolean selected) { this.selected = selected; }
}
