package com.dpnevsky.creditcalculator.application.api.rest.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmitApplicationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void allowsBlankAccountNumberWhenSalaryClientIsDisabled() {
        var violations = validator.validate(buildRequest(false, ""));

        assertTrue(violations.stream().noneMatch(violation ->
                violation.getMessage().contains("Account number is required")));
    }

    @Test
    void requiresAccountNumberWhenSalaryClientIsEnabled() {
        var violations = validator.validate(buildRequest(true, ""));

        assertTrue(violations.stream().anyMatch(violation ->
                violation.getMessage().contains("Account number is required")));
    }

    @Test
    void acceptsAccountNumberWhenSalaryClientIsEnabled() {
        var violations = validator.validate(buildRequest(true, "40817810099910004312"));

        assertFalse(violations.stream().anyMatch(violation ->
                violation.getMessage().contains("Account number is required")));
    }

    private SubmitApplicationRequest buildRequest(boolean salaryClient, String accountNumber) {
        return new SubmitApplicationRequest(
                false,
                salaryClient,
                SubmitApplicationRequest.GenderType.MALE,
                SubmitApplicationRequest.MaritalStatusType.SINGLE,
                1,
                LocalDate.of(2015, 1, 1),
                "770-001",
                accountNumber,
                new SubmitApplicationRequest.Employment(
                        SubmitApplicationRequest.EmploymentStatusType.EMPLOYED,
                        "7701234567",
                        new BigDecimal("120000.00"),
                        SubmitApplicationRequest.PositionType.OTHER,
                        24,
                        12
                )
        );
    }
}
