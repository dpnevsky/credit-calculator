package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.GetOfferResponse;
import com.dpnevsky.creditcalculator.application.application.exception.ApplicationNotFoundException;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.OfferRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetOffersService {

    private final ApplicationRepository applicationRepository;
    private final OfferRepository offerRepository;

    public GetOffersService(
            ApplicationRepository applicationRepository,
            OfferRepository offerRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.offerRepository = offerRepository;
    }

    public List<GetOfferResponse> getByApplicationId(UUID applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ApplicationNotFoundException(applicationId);
        }

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
