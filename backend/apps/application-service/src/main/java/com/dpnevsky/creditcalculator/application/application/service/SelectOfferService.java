package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.SelectOfferResponse;
import com.dpnevsky.creditcalculator.application.application.exception.ApplicationNotFoundException;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.OfferEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.OfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SelectOfferService {

    private static final String OFFER_SELECTED_STATUS = "OFFER_SELECTED";

    private final ApplicationRepository applicationRepository;
    private final OfferRepository offerRepository;

    public SelectOfferService(
            ApplicationRepository applicationRepository,
            OfferRepository offerRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.offerRepository = offerRepository;
    }

    @Transactional
    public SelectOfferResponse selectOffer(UUID applicationId, UUID offerId) {
        ApplicationEntity application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        String currentStatus = application.getStatus();
        if (!"SCORING_COMPLETED".equals(currentStatus)) {
            throw new IllegalStateException(
                    "Offer can only be selected for applications with status SCORING_COMPLETED, current: " + currentStatus
            );
        }

        OfferEntity offer = offerRepository.findByIdAndApplicationId(offerId, applicationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Offer not found: offerId=" + offerId + ", applicationId=" + applicationId
                ));

        offer.setSelected(true);
        offerRepository.save(offer);

        application.setStatus(OFFER_SELECTED_STATUS);
        application.setUpdatedAt(OffsetDateTime.now());
        applicationRepository.save(application);

        return new SelectOfferResponse(
                applicationId,
                offerId,
                OFFER_SELECTED_STATUS,
                "Offer selected successfully"
        );
    }
}
