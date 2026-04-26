package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.SelectOfferRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.SelectOfferResponse;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationSubmitDataEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.OfferEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationSubmitDataRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.OfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SelectOfferService {

    private static final String OFFER_SELECTED_STATUS = "OFFER_SELECTED";
    private static final String DEFAULT_PAYMENT_TYPE = "ANNUITY";
    private static final String SALARY_OFFER_ACCOUNT_NUMBER_ERROR =
            "Valid 20-digit account number is required for salary client offer";
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("\\d{20}");

    private final ApplicationAccessService applicationAccessService;
    private final ApplicationRepository applicationRepository;
    private final ApplicationSubmitDataRepository applicationSubmitDataRepository;
    private final OfferRepository offerRepository;

    public SelectOfferService(
            ApplicationAccessService applicationAccessService,
            ApplicationRepository applicationRepository,
            ApplicationSubmitDataRepository applicationSubmitDataRepository,
            OfferRepository offerRepository
    ) {
        this.applicationAccessService = applicationAccessService;
        this.applicationRepository = applicationRepository;
        this.applicationSubmitDataRepository = applicationSubmitDataRepository;
        this.offerRepository = offerRepository;
    }

    @Transactional
    public SelectOfferResponse selectOffer(
            UUID applicationId,
            String userEmail,
            UUID offerId,
            SelectOfferRequest request
    ) {
        ApplicationEntity application = applicationAccessService.getOwnedApplication(applicationId, userEmail);
        ensureOfferSelectionAllowed(application.getStatus());

        OfferEntity offer = getExistingOffer(applicationId, offerId);
        ensureSalaryOfferAccountNumberValid(offer, request, applicationId);

        offer.setSelected(true);
        offerRepository.save(offer);

        application.setStatus(OFFER_SELECTED_STATUS);
        application.setPaymentType(resolvePaymentType(application, request == null ? null : request.paymentType()));
        application.setUpdatedAt(OffsetDateTime.now());
        applicationRepository.save(application);

        return new SelectOfferResponse(
                applicationId,
                offerId,
                OFFER_SELECTED_STATUS,
                "Offer selected successfully"
        );
    }

    private void ensureOfferSelectionAllowed(String currentStatus) {
        if (!"SCORING_COMPLETED".equals(currentStatus)) {
            throw new IllegalStateException(
                    "Offer can only be selected for applications with status SCORING_COMPLETED, current: " + currentStatus
            );
        }
    }

    private OfferEntity getExistingOffer(UUID applicationId, UUID offerId) {
        return offerRepository.findByIdAndApplicationId(offerId, applicationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Offer not found: offerId=" + offerId + ", applicationId=" + applicationId
                ));
    }

    private void ensureSalaryOfferAccountNumberValid(
            OfferEntity offer,
            SelectOfferRequest request,
            UUID applicationId
    ) {
        if (!Boolean.TRUE.equals(offer.getSalaryClient())) {
            return;
        }

        String accountNumber = request == null ? null : request.accountNumber();
        if (accountNumber == null || !ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches()) {
            throw new IllegalStateException(SALARY_OFFER_ACCOUNT_NUMBER_ERROR);
        }

        ApplicationSubmitDataEntity submitData = applicationSubmitDataRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Application submit data not found: applicationId=" + applicationId
                ));
        submitData.setAccountNumber(accountNumber);
        applicationSubmitDataRepository.save(submitData);
    }

    private String resolvePaymentType(ApplicationEntity application, String requestedPaymentType) {
        if (requestedPaymentType != null && !requestedPaymentType.isBlank()) {
            return requestedPaymentType;
        }
        if (application.getPaymentType() != null && !application.getPaymentType().isBlank()) {
            return application.getPaymentType();
        }
        return DEFAULT_PAYMENT_TYPE;
    }
}
