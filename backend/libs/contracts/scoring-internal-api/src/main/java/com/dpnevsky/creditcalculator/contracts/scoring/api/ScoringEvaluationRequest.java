package com.dpnevsky.creditcalculator.contracts.scoring.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScoringEvaluationRequest(
        @NotNull
        UUID requestId,

        @NotNull
        UUID applicationId,

        @NotBlank
        String productCode,

        @NotNull
        OffsetDateTime requestedAt,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        String middleName,

        @NotNull
        GenderType gender,

        @NotNull
        @Past
        LocalDate birthDate,

        @NotBlank
        String passportSeries,

        @NotBlank
        String passportNumber,

        @NotNull
        @Past
        LocalDate passportIssueDate,

        @NotBlank
        String passportIssueBranch,

        @NotNull
        MaritalStatusType maritalStatus,

        @NotNull
        @Min(0)
        Integer dependentAmount,

        @NotNull
        @Valid
        Employment employment,

        @NotBlank
        String accountNumber,

        @NotNull
        @Valid
        LoanRequest loanRequest,

        @NotNull
        @Valid
        PrescoringSnapshot prescoringSnapshot
) {

    public enum GenderType {
        MALE,
        FEMALE,
        NON_BINARY
    }

    public enum MaritalStatusType {
        SINGLE,
        MARRIED,
        DIVORCED,
        WIDOWED
    }

    public enum EmploymentStatusType {
        EMPLOYED,
        UNEMPLOYED,
        SELF_EMPLOYED,
        RETIRED,
        BUSINESS_OWNER,
        STUDENT
    }

    public enum PositionType {
        MID_MANAGER,
        TOP_MANAGER,
        JUNIOR_MANAGER,
        DEVELOPER,
        SALES,
        ACCOUNTANT,
        HR,
        OTHER
    }

    public record Employment(
            @NotNull
            EmploymentStatusType employmentStatus,

            String employerInn,

            @NotNull
            @DecimalMin("0.00")
            BigDecimal salary,

            @NotNull
            PositionType position,

            @NotNull
            @Min(0)
            Integer workExperienceTotal,

            @NotNull
            @Min(0)
            Integer workExperienceCurrent
    ) {
    }

    public record LoanRequest(
            @NotNull
            @DecimalMin("20000.00")
            BigDecimal amount,

            @NotNull
            @Min(6)
            Integer termMonths,

            @NotBlank
            String currency,

            @NotNull
            Boolean insuranceEnabled,

            @NotNull
            Boolean salaryClient
    ) {
    }

    public record PrescoringSnapshot(
            @NotNull
            Boolean prescorePassed,

            @NotBlank
            String rulesVersion
    ) {
    }
}