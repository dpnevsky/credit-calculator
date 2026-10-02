package com.dpnevsky.creditcalculator.application.api.rest.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateApplicationRequestValidationTest {
    @ParameterizedTest
    @ValueSource(ints = {361, Integer.MAX_VALUE})
    void rejectsTermsBeyondTheSupportedScheduleLength(int term) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new CreateApplicationRequest(new BigDecimal("100000.00"), term,
                    "Ivan", "Ivanov", null, LocalDate.of(1990, 1, 1), "1234", "567890");
            assertTrue(factory.getValidator().validate(request).stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("termMonths")));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {6, 360})
    void acceptsSupportedTermBoundaries(int term) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new CreateApplicationRequest(new BigDecimal("100000.00"), term,
                    "Ivan", "Ivanov", null, LocalDate.of(1990, 1, 1), "1234", "567890");
            assertTrue(factory.getValidator().validate(request).isEmpty());
        }
    }
}
