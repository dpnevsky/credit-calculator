package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.domain.offers.LegacyPreOfferGenerationService;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.OfferEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.OfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CreatePreliminaryOffersService {

    private final LegacyPreOfferGenerationService legacyPreOfferGenerationService;
    private final OfferRepository offerRepository;

    public CreatePreliminaryOffersService(
            LegacyPreOfferGenerationService legacyPreOfferGenerationService,
            OfferRepository offerRepository
    ) {
        this.legacyPreOfferGenerationService = legacyPreOfferGenerationService;
        this.offerRepository = offerRepository;
    }

    @Transactional
    public List<LegacyPreOfferGenerationService.PreliminaryOffer> create(
            UUID applicationId,
            BigDecimal requestedAmount,
            Integer termMonths
    ) {
        offerRepository.deleteAllByApplicationId(applicationId);

        List<LegacyPreOfferGenerationService.PreliminaryOffer> offers =
                legacyPreOfferGenerationService.generateOffers(applicationId, requestedAmount, termMonths);

        OffsetDateTime now = OffsetDateTime.now();

        for (LegacyPreOfferGenerationService.PreliminaryOffer offer : offers) {
            OfferEntity entity = new OfferEntity(
                    UUID.randomUUID(),
                    applicationId,
                    offer.requestedAmount(),
                    offer.totalAmount(),
                    offer.termMonths(),
                    offer.monthlyPayment(),
                    offer.rate(),
                    offer.insuranceEnabled(),
                    offer.salaryClient(),
                    false,
                    now
            );
            offerRepository.save(entity);
        }

        return offers;
    }
}
