package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.ContractResponse;
import com.dpnevsky.creditcalculator.application.application.model.ContractStatus;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;

final class ContractResponseFactory {

    private static final String CONTRACT_NUMBER_PREFIX = "CC-";

    private ContractResponseFactory() {
    }

    static ContractResponse from(ApplicationEntity application) {
        return new ContractResponse(
                application.getId(),
                buildContractNumber(application),
                application.getContractStatus(),
                application.getStatus(),
                ContractStatus.SIGNED.name().equals(application.getContractStatus()),
                application.getContractSignedAt(),
                application.getSignatureId()
        );
    }

    private static String buildContractNumber(ApplicationEntity application) {
        return CONTRACT_NUMBER_PREFIX + application.getId().toString().substring(0, 8).toUpperCase();
    }
}
