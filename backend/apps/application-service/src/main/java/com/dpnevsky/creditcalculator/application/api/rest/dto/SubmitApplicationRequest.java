package com.dpnevsky.creditcalculator.application.api.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

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

        @NotBlank(message = "Код подразделения обязателен")
        @Pattern(
                regexp = "\\d{3}-\\d{3}",
                message = "Код подразделения должен быть в формате 000-000"
        )
        String passportIssueBranch,

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

    @AssertTrue(message = "Номер счета обязателен для зарплатного клиента")
    public boolean isAccountNumberValidForSalaryClient() {
        return !Boolean.TRUE.equals(salaryClient) || (accountNumber != null && !accountNumber.isBlank());
    }

    @AssertTrue(message = "ИНН работодателя должен содержать 10 или 12 цифр")
    public boolean isEmployerInnValid() {
        if (employment == null || employment.employmentStatus() == EmploymentStatusType.UNEMPLOYED) {
            return true;
        }

        String employerInn = employment.employerInn();
        return employerInn != null && employerInn.matches("\\d{10}|\\d{12}");
    }
}
