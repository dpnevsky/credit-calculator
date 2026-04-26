package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.ContractResponse;
import com.dpnevsky.creditcalculator.application.application.exception.ContractSigningConflictException;
import com.dpnevsky.creditcalculator.application.application.model.ApplicationStatus;
import com.dpnevsky.creditcalculator.application.application.model.ContractStatus;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SignContractService {

    private final ApplicationAccessService applicationAccessService;
    private final ApplicationRepository applicationRepository;

    public SignContractService(
            ApplicationAccessService applicationAccessService,
            ApplicationRepository applicationRepository
    ) {
        this.applicationAccessService = applicationAccessService;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public ContractResponse sign(UUID applicationId, String userEmail) {
        ApplicationEntity application = applicationAccessService.getOwnedApplicationForUpdate(applicationId, userEmail);

        if (ContractStatus.SIGNED.name().equals(application.getContractStatus())) {
            return ContractResponseFactory.from(application);
        }

        ensureContractCanBeSigned(application);

        OffsetDateTime signedAt = OffsetDateTime.now();
        application.setContractStatus(ContractStatus.SIGNED.name());
        application.setStatus(ApplicationStatus.CONTRACT_SIGNED.name());
        application.setContractSignedAt(signedAt);
        if (application.getSignatureId() == null) {
            application.setSignatureId(UUID.randomUUID());
        }
        application.setUpdatedAt(signedAt);

        applicationRepository.save(application);
        return ContractResponseFactory.from(application);
    }

    private void ensureContractCanBeSigned(ApplicationEntity application) {
        if (!ContractStatus.READY_TO_SIGN.name().equals(application.getContractStatus())) {
            throw new ContractSigningConflictException(
                    "Contract can be signed only when its status is READY_TO_SIGN"
            );
        }
    }
}
