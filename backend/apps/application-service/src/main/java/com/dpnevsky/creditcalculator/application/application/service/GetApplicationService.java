package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.GetApplicationResponse;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationSubmitDataEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationSubmitDataRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetApplicationService {

    private final ApplicationAccessService applicationAccessService;
    private final ApplicationSubmitDataRepository applicationSubmitDataRepository;

    public GetApplicationService(
            ApplicationAccessService applicationAccessService,
            ApplicationSubmitDataRepository applicationSubmitDataRepository
    ) {
        this.applicationAccessService = applicationAccessService;
        this.applicationSubmitDataRepository = applicationSubmitDataRepository;
    }

    public GetApplicationResponse getById(UUID applicationId, String userEmail) {
        ApplicationEntity applicationEntity = applicationAccessService.getOwnedApplication(applicationId, userEmail);
        GetApplicationResponse.SubmitData submitData = applicationSubmitDataRepository.findById(applicationId)
                .map(this::toSubmitData)
                .orElse(null);

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
                submitData
        );
    }

    private GetApplicationResponse.SubmitData toSubmitData(ApplicationSubmitDataEntity submitDataEntity) {
        return new GetApplicationResponse.SubmitData(
                submitDataEntity.getInsuranceEnabled(),
                submitDataEntity.getSalaryClient(),
                submitDataEntity.getGender(),
                submitDataEntity.getMaritalStatus(),
                submitDataEntity.getDependentAmount(),
                submitDataEntity.getPassportIssueDate(),
                submitDataEntity.getPassportIssueBranch(),
                submitDataEntity.getAccountNumber(),
                new GetApplicationResponse.Employment(
                        submitDataEntity.getEmploymentStatus(),
                        submitDataEntity.getEmployerInn(),
                        submitDataEntity.getSalary(),
                        submitDataEntity.getPosition(),
                        submitDataEntity.getWorkExperienceTotal(),
                        submitDataEntity.getWorkExperienceCurrent()
                ),
                submitDataEntity.getSubmittedAt()
        );
    }
}
