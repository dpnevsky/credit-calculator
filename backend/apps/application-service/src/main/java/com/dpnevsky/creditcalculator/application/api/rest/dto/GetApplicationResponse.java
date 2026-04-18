package com.dpnevsky.creditcalculator.application.api.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GetApplicationResponse(
        UUID applicationId,
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
        SubmitData submitData
) {
    public record SubmitData(
            Boolean insuranceEnabled,
            Boolean salaryClient,
            String gender,
            String maritalStatus,
            Integer dependentAmount,
            LocalDate passportIssueDate,
            String passportIssueBranch,
            String accountNumber,
            Employment employment,
            OffsetDateTime submittedAt
    ) {
    }

    public record Employment(
            String employmentStatus,
            String employerInn,
            BigDecimal salary,
            String position,
            Integer workExperienceTotal,
            Integer workExperienceCurrent
    ) {
    }
}
