package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.UpdateApplicationRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.UpdateApplicationResponse;
import com.dpnevsky.creditcalculator.application.domain.prescoring.LegacyPrescoringService;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UpdateApplicationService {

    private static final String UPDATED_STATUS = "DRAFT";
    private static final String PRESCORING_REJECTED_STATUS = "PRESCORING_REJECTED";

    private final ApplicationAccessService applicationAccessService;
    private final ApplicationRepository applicationRepository;
    private final LegacyPrescoringService legacyPrescoringService;

    public UpdateApplicationService(
            ApplicationAccessService applicationAccessService,
            ApplicationRepository applicationRepository,
            LegacyPrescoringService legacyPrescoringService
    ) {
        this.applicationAccessService = applicationAccessService;
        this.applicationRepository = applicationRepository;
        this.legacyPrescoringService = legacyPrescoringService;
    }

    @Transactional
    public UpdateApplicationResponse update(UUID applicationId, String userEmail, UpdateApplicationRequest request) {
        ApplicationEntity existingApplication = applicationAccessService.getOwnedApplication(applicationId, userEmail);

        LegacyPrescoringService.PrescoringResult prescoringResult = legacyPrescoringService.evaluate(
                request.amount(),
                request.termMonths(),
                request.birthDate()
        );

        String status = prescoringResult.passed()
                ? UPDATED_STATUS
                : PRESCORING_REJECTED_STATUS;

        OffsetDateTime now = OffsetDateTime.now();

        ApplicationEntity updatedApplication = new ApplicationEntity(
                existingApplication.getId(),
                status,
                request.amount(),
                request.termMonths(),
                request.firstName(),
                request.lastName(),
                request.middleName(),
                existingApplication.getEmail(),
                request.birthDate(),
                request.passportSeries(),
                request.passportNumber(),
                existingApplication.getCreatedAt(),
                now,
                existingApplication.getPaymentType()
        );

        applicationRepository.save(updatedApplication);

        if (!prescoringResult.passed()) {
            return new UpdateApplicationResponse(
                    applicationId,
                    PRESCORING_REJECTED_STATUS,
                    request.amount(),
                    request.termMonths(),
                    prescoringResult.reasons(),
                    List.of()
            );
        }

        return new UpdateApplicationResponse(
                applicationId,
                UPDATED_STATUS,
                request.amount(),
                request.termMonths(),
                List.of(),
                List.of()
        );
    }
}
