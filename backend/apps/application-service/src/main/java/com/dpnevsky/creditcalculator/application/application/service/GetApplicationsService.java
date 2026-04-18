package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationResponse;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetApplicationsService {

    private final ApplicationRepository applicationRepository;

    public GetApplicationsService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    public List<GetApplicationResponse> getByEmail(String email) {
        return applicationRepository.findAllByEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private GetApplicationResponse toResponse(ApplicationEntity applicationEntity) {
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
                applicationEntity.getCreatedAt(),
                applicationEntity.getUpdatedAt(),
                applicationEntity.getPaymentType(),
                null
        );
    }
}
