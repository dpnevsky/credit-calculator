package com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity;

import com.dpnevsky.creditcalculator.application.application.model.ContractStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "applications")
public class ApplicationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "passport_series", nullable = false, length = 32)
    private String passportSeries;

    @Column(name = "passport_number", nullable = false, length = 32)
    private String passportNumber;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "payment_type", nullable = false, length = 32)
    private String paymentType;

    @Column(name = "contract_status", nullable = false, length = 32)
    private String contractStatus;

    @Column(name = "contract_signed_at")
    private OffsetDateTime contractSignedAt;

    @Column(name = "signature_id", unique = true)
    private UUID signatureId;

    protected ApplicationEntity() {
        // for JPA
    }

    public ApplicationEntity(
            UUID id,
            String status,
            BigDecimal amount,
            Integer termMonths,
            String firstName,
            String lastName,
            String middleName,
            String email,
            LocalDate birthDate,
            String passportSeries,
            String passportNumber,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String paymentType
    ) {
        this(
                id,
                status,
                amount,
                termMonths,
                firstName,
                lastName,
                middleName,
                email,
                birthDate,
                passportSeries,
                passportNumber,
                createdAt,
                updatedAt,
                paymentType,
                ContractStatus.NOT_CREATED.name(),
                null,
                null
        );
    }

    public ApplicationEntity(
            UUID id,
            String status,
            BigDecimal amount,
            Integer termMonths,
            String firstName,
            String lastName,
            String middleName,
            String email,
            LocalDate birthDate,
            String passportSeries,
            String passportNumber,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String paymentType,
            String contractStatus,
            OffsetDateTime contractSignedAt,
            UUID signatureId
    ) {
        this.id = id;
        this.status = status;
        this.amount = amount;
        this.termMonths = termMonths;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.email = email;
        this.birthDate = birthDate;
        this.passportSeries = passportSeries;
        this.passportNumber = passportNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.paymentType = paymentType;
        this.contractStatus = contractStatus;
        this.contractSignedAt = contractSignedAt;
        this.signatureId = signatureId;
    }

    public UUID getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getPassportSeries() {
        return passportSeries;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public String getContractStatus() {
        return contractStatus;
    }

    public OffsetDateTime getContractSignedAt() {
        return contractSignedAt;
    }

    public UUID getSignatureId() {
        return signatureId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public void setContractStatus(String contractStatus) {
        this.contractStatus = contractStatus;
    }

    public void setContractSignedAt(OffsetDateTime contractSignedAt) {
        this.contractSignedAt = contractSignedAt;
    }

    public void setSignatureId(UUID signatureId) {
        this.signatureId = signatureId;
    }
}
