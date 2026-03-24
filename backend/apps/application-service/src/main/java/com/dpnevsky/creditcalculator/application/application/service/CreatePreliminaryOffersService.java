package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.domain.offers.LegacyPreOfferGenerationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CreatePreliminaryOffersService {

    private final LegacyPreOfferGenerationService legacyPreOfferGenerationService;

    public CreatePreliminaryOffersService(LegacyPreOfferGenerationService legacyPreOfferGenerationService) {
        this.legacyPreOfferGenerationService = legacyPreOfferGenerationService;
    }

    public List<LegacyPreOfferGenerationService.PreliminaryOffer> create(
            UUID applicationId,
            BigDecimal requestedAmount,
            Integer termMonths
    ) {
        return legacyPreOfferGenerationService.generateOffers(
                applicationId,
                requestedAmount,
                termMonths
        );
    }
}