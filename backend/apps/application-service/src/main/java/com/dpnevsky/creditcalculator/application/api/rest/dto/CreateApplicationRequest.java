package com.dpnevsky.creditcalculator.application.api.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateApplicationRequest(
        @NotNull
        @DecimalMin("20000.00")
        BigDecimal amount,

        @NotNull
        @Min(6)
        Integer termMonths,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        String middleName,

        @NotBlank
        @Email
        String email,

        @NotNull
        @Past
        LocalDate birthDate,

        @NotBlank
        String passportSeries,

        @NotBlank
        String passportNumber
) {
}