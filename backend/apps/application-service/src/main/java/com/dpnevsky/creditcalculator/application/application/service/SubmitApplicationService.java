package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.SubmitApplicationRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.SubmitApplicationResponse;
import com.dpnevsky.creditcalculator.application.application.exception.ApplicationNotFoundException;
import com.dpnevsky.creditcalculator.application.application.port.out.ScoringClient;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationSubmitDataEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ScoringSnapshotEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationSubmitDataRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ScoringSnapshotRepository;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationRequest;
import com.dpnevsky.creditcalculator.contracts.scoring.api.ScoringEvaluationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SubmitApplicationService {

    private static final String PRODUCT_CODE = "CREDIT_STANDARD";
    private static final String SCORING_COMPLETED_STATUS = "SCORING_COMPLETED";
    private static final String SCORING_REJECTED_STATUS = "SCORING_REJECTED";

    private final ApplicationRepository applicationRepository;
    private final ApplicationSubmitDataRepository applicationSubmitDataRepository;
    private final ScoringSnapshotRepository scoringSnapshotRepository;
    private final ScoringClient scoringClient;

    public SubmitApplicationService(
            ApplicationRepository applicationRepository,
            ApplicationSubmitDataRepository applicationSubmitDataRepository,
            ScoringSnapshotRepository scoringSnapshotRepository,
            ScoringClient scoringClient
    ) {
        this.applicationRepository = applicationRepository;
        this.applicationSubmitDataRepository = applicationSubmitDataRepository;
        this.scoringSnapshotRepository = scoringSnapshotRepository;
        this.scoringClient = scoringClient;
    }

    @Transactional
    public SubmitApplicationResponse submit(UUID applicationId, SubmitApplicationRequest request) {
        ApplicationEntity existingApplication = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        if ("PRESCORING_REJECTED".equals(existingApplication.getStatus())) {
            throw new IllegalStateException("Application did not pass prescoring and cannot be submitted");
        }

        OffsetDateTime now = OffsetDateTime.now();

        ScoringEvaluationRequest scoringRequest = new ScoringEvaluationRequest(
                UUID.randomUUID(),
                existingApplication.getId(),
                PRODUCT_CODE,
                now,

                existingApplication.getFirstName(),
                existingApplication.getLastName(),
                existingApplication.getMiddleName(),

                ScoringEvaluationRequest.GenderType.valueOf(request.gender().name()),
                existingApplication.getBirthDate(),

                existingApplication.getPassportSeries(),
                existingApplication.getPassportNumber(),
                request.passportIssueDate(),
                request.passportIssueBranch(),

                ScoringEvaluationRequest.MaritalStatusType.valueOf(request.maritalStatus().name()),
                request.dependentAmount(),

                new ScoringEvaluationRequest.Employment(
                        ScoringEvaluationRequest.EmploymentStatusType.valueOf(request.employment().employmentStatus().name()),
                        request.employment().employerInn(),
                        request.employment().salary(),
                        ScoringEvaluationRequest.PositionType.valueOf(request.employment().position().name()),
                        request.employment().workExperienceTotal(),
                        request.employment().workExperienceCurrent()
                ),

                request.accountNumber(),

                new ScoringEvaluationRequest.LoanRequest(
                        existingApplication.getAmount(),
                        existingApplication.getTermMonths(),
                        "RUB",
                        request.insuranceEnabled(),
                        request.salaryClient()
                ),

                new ScoringEvaluationRequest.PrescoringSnapshot(
                        true,
                        "prescore-v1"
                )
        );

        ScoringEvaluationResponse scoringResponse = scoringClient.evaluate(scoringRequest);

        ApplicationSubmitDataEntity submitDataEntity = new ApplicationSubmitDataEntity(
                existingApplication.getId(),
                request.insuranceEnabled(),
                request.salaryClient(),
                request.gender().name(),
                request.maritalStatus().name(),
                request.dependentAmount(),
                request.passportIssueDate(),
                request.passportIssueBranch(),
                request.accountNumber(),
                request.employment().employmentStatus().name(),
                request.employment().employerInn(),
                request.employment().salary(),
                request.employment().position().name(),
                request.employment().workExperienceTotal(),
                request.employment().workExperienceCurrent(),
                now
        );
        applicationSubmitDataRepository.save(submitDataEntity);

        String applicationStatus = "APPROVED".equals(scoringResponse.decision())
                ? SCORING_COMPLETED_STATUS
                : SCORING_REJECTED_STATUS;

        ApplicationEntity updatedApplication = new ApplicationEntity(
                existingApplication.getId(),
                applicationStatus,
                existingApplication.getAmount(),
                existingApplication.getTermMonths(),
                existingApplication.getFirstName(),
                existingApplication.getLastName(),
                existingApplication.getMiddleName(),
                existingApplication.getEmail(),
                existingApplication.getBirthDate(),
                existingApplication.getPassportSeries(),
                existingApplication.getPassportNumber(),
                existingApplication.getCreatedAt(),
                now,
                existingApplication.getPaymentType()
        );
        applicationRepository.save(updatedApplication);

        ScoringSnapshotEntity scoringSnapshotEntity = new ScoringSnapshotEntity(
                UUID.randomUUID(),
                existingApplication.getId(),
                scoringResponse.decision(),
                scoringResponse.scoreValue(),
                scoringResponse.riskGrade(),
                scoringResponse.rulesVersion(),
                scoringResponse.maxApprovedAmount(),
                scoringResponse.maxTermMonths(),
                scoringResponse.baseInterestRate(),
                scoringResponse.reasons(),
                scoringResponse.calculatedAt()
        );
        scoringSnapshotRepository.save(scoringSnapshotEntity);

        return new SubmitApplicationResponse(
                existingApplication.getId(),
                applicationStatus,
                scoringResponse.decision(),
                scoringResponse.scoreValue(),
                scoringResponse.riskGrade(),
                scoringResponse.rulesVersion(),
                scoringResponse.maxApprovedAmount(),
                scoringResponse.maxTermMonths(),
                scoringResponse.baseInterestRate(),
                scoringResponse.reasons()
        );
    }
}
