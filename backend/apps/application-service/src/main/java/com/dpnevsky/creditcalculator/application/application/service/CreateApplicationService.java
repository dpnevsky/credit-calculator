package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.CreateApplicationRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.CreateApplicationResponse;
import com.dpnevsky.creditcalculator.application.api.rest.mapper.PreliminaryOfferResponseMapper;
import com.dpnevsky.creditcalculator.application.domain.prescoring.LegacyPrescoringService;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CreateApplicationService {

    private static final String INITIAL_STATUS = "DRAFT";
    private static final String PRESCORING_REJECTED_STATUS = "PRESCORING_REJECTED";
    private static final String DEFAULT_PAYMENT_TYPE = "ANNUITY";

    private final CreatePreliminaryOffersService createPreliminaryOffersService;
    private final PreliminaryOfferResponseMapper preliminaryOfferResponseMapper;
    private final LegacyPrescoringService legacyPrescoringService;
    private final ApplicationRepository applicationRepository;

    public CreateApplicationService(
            CreatePreliminaryOffersService createPreliminaryOffersService,
            PreliminaryOfferResponseMapper preliminaryOfferResponseMapper,
            LegacyPrescoringService legacyPrescoringService,
            ApplicationRepository applicationRepository
    ) {
        this.createPreliminaryOffersService = createPreliminaryOffersService;
        this.preliminaryOfferResponseMapper = preliminaryOfferResponseMapper;
        this.legacyPrescoringService = legacyPrescoringService;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public CreateApplicationResponse create(CreateApplicationRequest request) {
        UUID applicationId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        LegacyPrescoringService.PrescoringResult prescoringResult = legacyPrescoringService.evaluate(
                request.amount(),
                request.termMonths(),
                request.birthDate()
        );

        String status = prescoringResult.passed()
                ? INITIAL_STATUS
                : PRESCORING_REJECTED_STATUS;

        ApplicationEntity applicationEntity = new ApplicationEntity(
                applicationId,
                status,
                request.amount(),
                request.termMonths(),
                request.firstName(),
                request.lastName(),
                request.middleName(),
                request.email(),
                request.birthDate(),
                request.passportSeries(),
                request.passportNumber(),
                now,
                now,
                DEFAULT_PAYMENT_TYPE
        );
        applicationRepository.save(applicationEntity);

        if (!prescoringResult.passed()) {
            return new CreateApplicationResponse(
                    applicationId,
                    PRESCORING_REJECTED_STATUS,
                    request.amount(),
                    request.termMonths(),
                    prescoringResult.reasons(),
                    List.of()
            );
        }

        var offers = createPreliminaryOffersService.create(
                applicationId,
                request.amount(),
                request.termMonths()
        );

        return new CreateApplicationResponse(
                applicationId,
                INITIAL_STATUS,
                request.amount(),
                request.termMonths(),
                List.of(),
                preliminaryOfferResponseMapper.toResponseList(offers)
        );
    }
}
