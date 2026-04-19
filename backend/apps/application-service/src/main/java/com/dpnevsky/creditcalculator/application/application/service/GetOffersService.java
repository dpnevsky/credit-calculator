package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.GetOfferResponse;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.OfferRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetOffersService {

    private final ApplicationAccessService applicationAccessService;
    private final OfferRepository offerRepository;

    public GetOffersService(
            ApplicationAccessService applicationAccessService,
            OfferRepository offerRepository
    ) {
        this.applicationAccessService = applicationAccessService;
        this.offerRepository = offerRepository;
    }

    public List<GetOfferResponse> getByApplicationId(UUID applicationId, String userEmail) {
        applicationAccessService.getOwnedApplication(applicationId, userEmail);

        return offerRepository.findAllByApplicationIdOrderByRateAsc(applicationId)
                .stream()
                .map(offer -> new GetOfferResponse(
                        offer.getId(),
                        offer.getApplicationId(),
                        offer.getRequestedAmount(),
                        offer.getTotalAmount(),
                        offer.getTermMonths(),
                        offer.getMonthlyPayment(),
                        offer.getRate(),
                        offer.getInsuranceEnabled(),
                        offer.getSalaryClient(),
                        offer.getSelected(),
                        offer.getCreatedAt()
                ))
                .toList();
    }
}
