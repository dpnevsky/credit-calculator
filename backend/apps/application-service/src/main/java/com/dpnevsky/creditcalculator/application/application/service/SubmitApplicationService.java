package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.SubmitApplicationRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.SubmitApplicationResponse;
import com.dpnevsky.creditcalculator.application.application.port.out.ScoringClient;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationSubmitDataEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ScoringSnapshotEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationSubmitDataRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.OfferRepository;
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

    private final ApplicationAccessService applicationAccessService;
    private final ApplicationRepository applicationRepository;
    private final ApplicationSubmitDataRepository applicationSubmitDataRepository;
    private final ScoringSnapshotRepository scoringSnapshotRepository;
    private final OfferRepository offerRepository;
    private final CreatePreliminaryOffersService createPreliminaryOffersService;
    private final ScoringClient scoringClient;

    public SubmitApplicationService(
            ApplicationAccessService applicationAccessService,
            ApplicationRepository applicationRepository,
            ApplicationSubmitDataRepository applicationSubmitDataRepository,
            ScoringSnapshotRepository scoringSnapshotRepository,
            OfferRepository offerRepository,
            CreatePreliminaryOffersService createPreliminaryOffersService,
            ScoringClient scoringClient
    ) {
        this.applicationAccessService = applicationAccessService;
        this.applicationRepository = applicationRepository;
        this.applicationSubmitDataRepository = applicationSubmitDataRepository;
        this.scoringSnapshotRepository = scoringSnapshotRepository;
        this.offerRepository = offerRepository;
        this.createPreliminaryOffersService = createPreliminaryOffersService;
        this.scoringClient = scoringClient;
    }

    @Transactional
    public SubmitApplicationResponse submit(UUID applicationId, String userEmail, SubmitApplicationRequest request) {
        ApplicationEntity existingApplication = applicationAccessService.getOwnedApplication(applicationId, userEmail);

        if (!"DRAFT".equals(existingApplication.getStatus())) {
            throw new IllegalStateException("Only draft applications can be submitted for scoring");
        }

        OffsetDateTime now = OffsetDateTime.now();
        String normalizedAccountNumber = normalizeAccountNumber(request.accountNumber());

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

                normalizedAccountNumber,

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
                normalizedAccountNumber,
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

        if ("APPROVED".equals(scoringResponse.decision())) {
            createPreliminaryOffersService.create(
                    existingApplication.getId(),
                    scoringResponse.maxApprovedAmount(),
                    scoringResponse.maxTermMonths(),
                    scoringResponse.baseInterestRate()
            );
        } else {
            offerRepository.deleteAllByApplicationId(existingApplication.getId());
        }

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
                existingApplication.getPaymentType(),
                existingApplication.getContractStatus(),
                existingApplication.getContractSignedAt(),
                existingApplication.getSignatureId()
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

    private String normalizeAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }
        return accountNumber;
    }
}
