package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanScoringData(
        BigDecimal amount,
        LocalDate birthdate,
        GenderType gender,
        MaritalStatusType maritalStatus,
        boolean insuranceEnabled,
        boolean salaryClient,
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
            EmploymentStatusType employmentStatus,
            BigDecimal salary,
            PositionType position,
            Integer workExperienceTotal,
            Integer workExperienceCurrent
    ) {
    }
}
