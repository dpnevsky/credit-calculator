package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.ContractResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetContractService {

    private final ApplicationAccessService applicationAccessService;

    public GetContractService(ApplicationAccessService applicationAccessService) {
        this.applicationAccessService = applicationAccessService;
    }

    public ContractResponse getByApplicationId(UUID applicationId, String userEmail) {
        return ContractResponseFactory.from(applicationAccessService.getOwnedApplication(applicationId, userEmail));
    }
}
