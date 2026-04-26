package com.dpnevsky.creditcalculator.application.api.rest.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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
                violation.getMessage().contains("Номер счета обязателен")));
    }

    @Test
    void requiresAccountNumberWhenSalaryClientIsEnabled() {
        var violations = validator.validate(buildRequest(true, ""));

        assertTrue(violations.stream().anyMatch(violation ->
                violation.getMessage().contains("Номер счета обязателен")));
    }

    @Test
    void acceptsAccountNumberWhenSalaryClientIsEnabled() {
        var violations = validator.validate(buildRequest(true, "40817810099910004312"));

        assertFalse(violations.stream().anyMatch(violation ->
                violation.getMessage().contains("Номер счета обязателен")));
    }

    @Test
    void acceptsValidPassportIssueBranch() {
        var violations = validator.validate(buildRequest(false, "", "123-456"));

        assertFalse(violations.stream().anyMatch(violation ->
                violation.getMessage().contains("Код подразделения")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123456", "12-3456", "123-45", "abc-def", "123 456"})
    void rejectsInvalidPassportIssueBranch(String passportIssueBranch) {
        var violations = validator.validate(buildRequest(false, "", passportIssueBranch));

        assertTrue(violations.stream().anyMatch(violation ->
                violation.getMessage().contains("Код подразделения")));
    }

    private SubmitApplicationRequest buildRequest(boolean salaryClient, String accountNumber) {
        return buildRequest(salaryClient, accountNumber, "770-001");
    }

    private SubmitApplicationRequest buildRequest(
            boolean salaryClient,
            String accountNumber,
            String passportIssueBranch
    ) {
        return new SubmitApplicationRequest(
                false,
                salaryClient,
                SubmitApplicationRequest.GenderType.MALE,
                SubmitApplicationRequest.MaritalStatusType.SINGLE,
                1,
                LocalDate.of(2015, 1, 1),
                passportIssueBranch,
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
