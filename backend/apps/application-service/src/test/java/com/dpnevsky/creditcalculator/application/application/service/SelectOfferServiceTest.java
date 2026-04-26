package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.SelectOfferRequest;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationSubmitDataEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.OfferEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationSubmitDataRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelectOfferServiceTest {

    @Test
    void rejectsSelectingOfferForDraftApplication() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        ApplicationSubmitDataRepository applicationSubmitDataRepository = mock(ApplicationSubmitDataRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        SelectOfferService service = new SelectOfferService(
                applicationAccessService,
                applicationRepository,
                applicationSubmitDataRepository,
                offerRepository
        );
        UUID applicationId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();

        when(applicationAccessService.getOwnedApplication(applicationId, "owner@example.com"))
                .thenReturn(buildApplication(applicationId, "DRAFT"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.selectOffer(
                        applicationId,
                        "owner@example.com",
                        offerId,
                        new SelectOfferRequest("DIFFERENTIAL", null)
                )
        );

        assertEquals(
                "Offer can only be selected for applications with status SCORING_COMPLETED, current: DRAFT",
                exception.getMessage()
        );
    }

    @Test
    void allowsSelectingOfferAfterScoringCompleted() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        ApplicationSubmitDataRepository applicationSubmitDataRepository = mock(ApplicationSubmitDataRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        SelectOfferService service = new SelectOfferService(
                applicationAccessService,
                applicationRepository,
                applicationSubmitDataRepository,
                offerRepository
        );
        UUID applicationId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        OfferEntity offer = buildOffer(applicationId, offerId, false);

        when(applicationAccessService.getOwnedApplication(applicationId, "owner@example.com"))
                .thenReturn(buildApplication(applicationId, "SCORING_COMPLETED"));
        when(offerRepository.findByIdAndApplicationId(offerId, applicationId)).thenReturn(Optional.of(offer));

        var response = service.selectOffer(
                applicationId,
                "owner@example.com",
                offerId,
                new SelectOfferRequest("DIFFERENTIAL", null)
        );

        assertEquals("OFFER_SELECTED", response.applicationStatus());
        verify(offerRepository).save(offer);
        verify(applicationRepository).save(org.mockito.ArgumentMatchers.argThat(
                application -> "DIFFERENTIAL".equals(application.getPaymentType())
                        && "OFFER_SELECTED".equals(application.getStatus())
        ));
    }

    @Test
    void allowsSelectingNonSalaryOfferWithoutRequestBody() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        ApplicationSubmitDataRepository applicationSubmitDataRepository = mock(ApplicationSubmitDataRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        SelectOfferService service = new SelectOfferService(
                applicationAccessService,
                applicationRepository,
                applicationSubmitDataRepository,
                offerRepository
        );
        UUID applicationId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        OfferEntity offer = buildOffer(applicationId, offerId, false);

        when(applicationAccessService.getOwnedApplication(applicationId, "owner@example.com"))
                .thenReturn(buildApplication(applicationId, "SCORING_COMPLETED"));
        when(offerRepository.findByIdAndApplicationId(offerId, applicationId)).thenReturn(Optional.of(offer));

        var response = service.selectOffer(applicationId, "owner@example.com", offerId, null);

        assertEquals("OFFER_SELECTED", response.applicationStatus());
        verify(offerRepository).save(offer);
        verify(applicationSubmitDataRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void selectingSalaryOfferWithValidAccountNumberSavesAccountNumber() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        ApplicationSubmitDataRepository applicationSubmitDataRepository = mock(ApplicationSubmitDataRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        SelectOfferService service = new SelectOfferService(
                applicationAccessService,
                applicationRepository,
                applicationSubmitDataRepository,
                offerRepository
        );
        UUID applicationId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        OfferEntity offer = buildOffer(applicationId, offerId, true);
        ApplicationSubmitDataEntity submitData = buildSubmitData(applicationId, null);

        when(applicationAccessService.getOwnedApplication(applicationId, "owner@example.com"))
                .thenReturn(buildApplication(applicationId, "SCORING_COMPLETED"));
        when(offerRepository.findByIdAndApplicationId(offerId, applicationId)).thenReturn(Optional.of(offer));
        when(applicationSubmitDataRepository.findById(applicationId)).thenReturn(Optional.of(submitData));

        var response = service.selectOffer(
                applicationId,
                "owner@example.com",
                offerId,
                new SelectOfferRequest("ANNUITY", "40817810099910004312")
        );

        assertEquals("OFFER_SELECTED", response.applicationStatus());
        assertEquals("40817810099910004312", submitData.getAccountNumber());
        verify(applicationSubmitDataRepository).save(submitData);
        verify(offerRepository).save(offer);
        verify(applicationRepository).save(org.mockito.ArgumentMatchers.argThat(
                application -> "OFFER_SELECTED".equals(application.getStatus())
        ));
    }

    @Test
    void rejectsSelectingSalaryOfferWithNullAccountNumber() {
        assertSalaryOfferAccountNumberRejected(null);
    }

    @Test
    void rejectsSelectingSalaryOfferWithBlankAccountNumber() {
        assertSalaryOfferAccountNumberRejected(" ");
    }

    @Test
    void rejectsSelectingSalaryOfferWithShortAccountNumber() {
        assertSalaryOfferAccountNumberRejected("4081781009991000431");
    }

    @Test
    void rejectsSelectingSalaryOfferWithLettersInAccountNumber() {
        assertSalaryOfferAccountNumberRejected("4081781009991000431A");
    }

    private void assertSalaryOfferAccountNumberRejected(String accountNumber) {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        ApplicationSubmitDataRepository applicationSubmitDataRepository = mock(ApplicationSubmitDataRepository.class);
        OfferRepository offerRepository = mock(OfferRepository.class);
        SelectOfferService service = new SelectOfferService(
                applicationAccessService,
                applicationRepository,
                applicationSubmitDataRepository,
                offerRepository
        );
        UUID applicationId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        ApplicationEntity application = buildApplication(applicationId, "SCORING_COMPLETED");
        OfferEntity offer = buildOffer(applicationId, offerId, true);

        when(applicationAccessService.getOwnedApplication(applicationId, "owner@example.com"))
                .thenReturn(application);
        when(offerRepository.findByIdAndApplicationId(offerId, applicationId)).thenReturn(Optional.of(offer));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.selectOffer(
                        applicationId,
                        "owner@example.com",
                        offerId,
                        new SelectOfferRequest("ANNUITY", accountNumber)
                )
        );

        assertEquals("Valid 20-digit account number is required for salary client offer", exception.getMessage());
        assertEquals(false, offer.getSelected());
        assertEquals("SCORING_COMPLETED", application.getStatus());
        verify(applicationSubmitDataRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(offerRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(applicationRepository, never()).save(org.mockito.ArgumentMatchers.any());
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

    private OfferEntity buildOffer(UUID applicationId, UUID offerId, boolean salaryClient) {
        return new OfferEntity(
                offerId,
                applicationId,
                new BigDecimal("500000.00"),
                new BigDecimal("500000.00"),
                12,
                new BigDecimal("45000.00"),
                new BigDecimal("12.00"),
                false,
                salaryClient,
                false,
                OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC)
        );
    }

    private ApplicationSubmitDataEntity buildSubmitData(UUID applicationId, String accountNumber) {
        return new ApplicationSubmitDataEntity(
                applicationId,
                false,
                true,
                "MALE",
                "SINGLE",
                0,
                LocalDate.of(2020, 1, 1),
                "770-001",
                accountNumber,
                "EMPLOYED",
                "7700000000",
                new BigDecimal("100000.00"),
                "DEVELOPER",
                60,
                24,
                OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC)
        );
    }
}
