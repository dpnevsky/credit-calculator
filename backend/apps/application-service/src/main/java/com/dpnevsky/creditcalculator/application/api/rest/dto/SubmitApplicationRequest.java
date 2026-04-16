package com.dpnevsky.creditcalculator.application.api.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitApplicationRequest(
        @NotNull
        Boolean insuranceEnabled,

        @NotNull
        Boolean salaryClient,

        @NotNull
        GenderType gender,

        @NotNull
        MaritalStatusType maritalStatus,

        @NotNull
        @Min(0)
        Integer dependentAmount,

        @NotNull
        @Past
        LocalDate passportIssueDate,

        @NotBlank
        String passportIssueBranch,

        @NotBlank
        String accountNumber,

        @NotNull
        @Valid
        Employment employment
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
}
