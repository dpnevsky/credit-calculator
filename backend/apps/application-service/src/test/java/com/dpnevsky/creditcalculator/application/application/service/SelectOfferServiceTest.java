package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.OfferEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.OfferRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelectOfferServiceTest {

    @Test
    void rejectsSelectingOfferForDraftApplication() {
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        SelectOfferService service = new SelectOfferService(applicationRepository, offerRepository);
        UUID applicationId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(buildApplication(applicationId, "DRAFT")));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.selectOffer(applicationId, offerId)
        );

        assertEquals(
                "Offer can only be selected for applications with status SCORING_COMPLETED, current: DRAFT",
                exception.getMessage()
        );
    }

    @Test
    void allowsSelectingOfferAfterScoringCompleted() {
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        SelectOfferService service = new SelectOfferService(applicationRepository, offerRepository);
        UUID applicationId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        OfferEntity offer = buildOffer(applicationId, offerId);

        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.of(buildApplication(applicationId, "SCORING_COMPLETED")));
        when(offerRepository.findByIdAndApplicationId(offerId, applicationId)).thenReturn(Optional.of(offer));

        var response = service.selectOffer(applicationId, offerId);

        assertEquals("OFFER_SELECTED", response.applicationStatus());
        verify(offerRepository).save(offer);
        verify(applicationRepository).save(org.mockito.ArgumentMatchers.any());
    }

    private ApplicationEntity buildApplication(UUID applicationId, String status) {
        OffsetDateTime now = OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC);
        return new ApplicationEntity(
                applicationId,
                status,
                new BigDecimal("500000.00"),
                12,
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "ivan@example.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "567890",
                now,
                now,
                "ANNUITY"
        );
    }

    private OfferEntity buildOffer(UUID applicationId, UUID offerId) {
        return new OfferEntity(
                offerId,
                applicationId,
                new BigDecimal("500000.00"),
                new BigDecimal("500000.00"),
                12,
                new BigDecimal("45000.00"),
                new BigDecimal("12.00"),
                false,
                false,
                false,
                OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC)
        );
    }
}
