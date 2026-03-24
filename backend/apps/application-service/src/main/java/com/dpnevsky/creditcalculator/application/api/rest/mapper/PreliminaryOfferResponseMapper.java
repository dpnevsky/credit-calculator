package com.dpnevsky.creditcalculator.application.api.rest.mapper;

import com.dpnevsky.creditcalculator.application.api.rest.dto.PreliminaryOfferResponse;
import com.dpnevsky.creditcalculator.application.domain.offers.LegacyPreOfferGenerationService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PreliminaryOfferResponseMapper {

    public PreliminaryOfferResponse toResponse(LegacyPreOfferGenerationService.PreliminaryOffer offer) {
        return new PreliminaryOfferResponse(
                offer.applicationId(),
                offer.requestedAmount(),
                offer.totalAmount(),
                offer.termMonths(),
                offer.monthlyPayment(),
                offer.rate(),
                offer.insuranceEnabled(),
                offer.salaryClient()
        );
    }

    public List<PreliminaryOfferResponse> toResponseList(
            List<LegacyPreOfferGenerationService.PreliminaryOffer> offers
    ) {
        return offers.stream()
                .map(this::toResponse)
                .toList();
    }
}