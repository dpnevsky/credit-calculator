package com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_submit_data")
public class ApplicationSubmitDataEntity {

    @Id
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "insurance_enabled", nullable = false)
    private Boolean insuranceEnabled;

    @Column(name = "salary_client", nullable = false)
    private Boolean salaryClient;

    @Column(name = "gender", nullable = false, length = 32)
    private String gender;

    @Column(name = "marital_status", nullable = false, length = 32)
    private String maritalStatus;

    @Column(name = "dependent_amount", nullable = false)
    private Integer dependentAmount;

    @Column(name = "passport_issue_date", nullable = false)
    private LocalDate passportIssueDate;

    @Column(name = "passport_issue_branch", nullable = false, length = 255)
    private String passportIssueBranch;

    @Column(name = "account_number", length = 64)
    private String accountNumber;

    @Column(name = "employment_status", nullable = false, length = 32)
    private String employmentStatus;

    @Column(name = "employer_inn", length = 32)
    private String employerInn;

    @Column(name = "salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal salary;

    @Column(name = "position", nullable = false, length = 32)
    private String position;

    @Column(name = "work_experience_total", nullable = false)
    private Integer workExperienceTotal;

    @Column(name = "work_experience_current", nullable = false)
    private Integer workExperienceCurrent;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    protected ApplicationSubmitDataEntity() {
        // for JPA
    }

    public ApplicationSubmitDataEntity(
            UUID applicationId,
            Boolean insuranceEnabled,
            Boolean salaryClient,
            String gender,
            String maritalStatus,
            Integer dependentAmount,
            LocalDate passportIssueDate,
            String passportIssueBranch,
            String accountNumber,
            String employmentStatus,
            String employerInn,
            BigDecimal salary,
            String position,
            Integer workExperienceTotal,
            Integer workExperienceCurrent,
            OffsetDateTime submittedAt
    ) {
        this.applicationId = applicationId;
        this.insuranceEnabled = insuranceEnabled;
        this.salaryClient = salaryClient;
        this.gender = gender;
        this.maritalStatus = maritalStatus;
        this.dependentAmount = dependentAmount;
        this.passportIssueDate = passportIssueDate;
        this.passportIssueBranch = passportIssueBranch;
        this.accountNumber = accountNumber;
        this.employmentStatus = employmentStatus;
        this.employerInn = employerInn;
        this.salary = salary;
        this.position = position;
        this.workExperienceTotal = workExperienceTotal;
        this.workExperienceCurrent = workExperienceCurrent;
        this.submittedAt = submittedAt;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public Boolean getInsuranceEnabled() {
        return insuranceEnabled;
    }

    public Boolean getSalaryClient() {
        return salaryClient;
    }

    public String getGender() {
        return gender;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public Integer getDependentAmount() {
        return dependentAmount;
    }

    public LocalDate getPassportIssueDate() {
        return passportIssueDate;
    }

    public String getPassportIssueBranch() {
        return passportIssueBranch;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public String getEmployerInn() {
        return employerInn;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public String getPosition() {
        return position;
    }

    public Integer getWorkExperienceTotal() {
        return workExperienceTotal;
    }

    public Integer getWorkExperienceCurrent() {
        return workExperienceCurrent;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }
}
