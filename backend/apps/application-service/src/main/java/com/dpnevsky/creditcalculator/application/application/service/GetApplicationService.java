package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationResponse;
import com.dpnevsky.creditcalculator.application.application.exception.ApplicationNotFoundException;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetApplicationService {

    private final ApplicationRepository applicationRepository;

    public GetApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    public GetApplicationResponse getById(UUID applicationId) {
        ApplicationEntity applicationEntity = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        return new GetApplicationResponse(
                applicationEntity.getId(),
                applicationEntity.getStatus(),
                applicationEntity.getAmount(),
                applicationEntity.getTermMonths(),
                applicationEntity.getFirstName(),
                applicationEntity.getLastName(),
                applicationEntity.getMiddleName(),
                applicationEntity.getEmail(),
                applicationEntity.getBirthDate(),
                applicationEntity.getPassportSeries(),
                applicationEntity.getPassportNumber(),
                applicationEntity.getCreatedAt()
        );
    }
}