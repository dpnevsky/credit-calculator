package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.application.exception.ApplicationNotFoundException;
import com.dpnevsky.creditcalculator.application.application.exception.ContractSigningConflictException;
import com.dpnevsky.creditcalculator.application.application.model.ContractStatus;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignContractServiceTest {

    @Test
    void signsReadyContractAndPersistsSignatureMetadata() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        SignContractService service = new SignContractService(applicationAccessService, applicationRepository);
        UUID applicationId = UUID.randomUUID();
        ApplicationEntity application = buildApplication(
                applicationId,
                "DOCUMENTS_READY",
                ContractStatus.READY_TO_SIGN.name(),
                null,
                null
        );

        when(applicationAccessService.getOwnedApplicationForUpdate(applicationId, "owner@example.com"))
                .thenReturn(application);

        var response = service.sign(applicationId, "owner@example.com");

        assertEquals("CONTRACT_SIGNED", response.applicationStatus());
        assertEquals(ContractStatus.SIGNED.name(), response.contractStatus());
        assertEquals(true, response.signed());
        assertNotNull(response.signedAt());
        assertNotNull(response.signatureId());
        verify(applicationRepository).save(argThat(savedApplication ->
                "CONTRACT_SIGNED".equals(savedApplication.getStatus())
                        && ContractStatus.SIGNED.name().equals(savedApplication.getContractStatus())
                        && savedApplication.getContractSignedAt() != null
                        && savedApplication.getSignatureId() != null
        ));
    }

    @Test
    void returnsIdempotentSignedResponseWithoutSavingAgain() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        SignContractService service = new SignContractService(applicationAccessService, applicationRepository);
        UUID applicationId = UUID.randomUUID();
        AtomicReference<ApplicationEntity> storedApplication = new AtomicReference<>(buildApplication(
                applicationId,
                "DOCUMENTS_READY",
                ContractStatus.READY_TO_SIGN.name(),
                null,
                null
        ));

        when(applicationAccessService.getOwnedApplicationForUpdate(applicationId, "owner@example.com"))
                .thenAnswer(invocation -> storedApplication.get());
        when(applicationRepository.save(any(ApplicationEntity.class)))
                .thenAnswer(invocation -> {
                    ApplicationEntity savedApplication = invocation.getArgument(0);
                    storedApplication.set(savedApplication);
                    return savedApplication;
                });

        var firstResponse = service.sign(applicationId, "owner@example.com");
        var secondResponse = service.sign(applicationId, "owner@example.com");

        assertEquals(firstResponse.signatureId(), secondResponse.signatureId());
        assertEquals(firstResponse.signedAt(), secondResponse.signedAt());
        verify(applicationRepository, times(1)).save(any(ApplicationEntity.class));
    }

    @Test
    void rejectsSigningWhenContractIsNotReady() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        SignContractService service = new SignContractService(applicationAccessService, applicationRepository);
        UUID applicationId = UUID.randomUUID();

        when(applicationAccessService.getOwnedApplicationForUpdate(applicationId, "owner@example.com"))
                .thenReturn(buildApplication(
                        applicationId,
                        "OFFER_SELECTED",
                        ContractStatus.NOT_CREATED.name(),
                        null,
                        null
                ));

        ContractSigningConflictException exception = assertThrows(
                ContractSigningConflictException.class,
                () -> service.sign(applicationId, "owner@example.com")
        );

        assertEquals("Contract can be signed only when its status is READY_TO_SIGN", exception.getMessage());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void propagatesApplicationNotFoundForUnknownApplication() {
        ApplicationAccessService applicationAccessService = mock(ApplicationAccessService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        SignContractService service = new SignContractService(applicationAccessService, applicationRepository);
        UUID applicationId = UUID.randomUUID();

        when(applicationAccessService.getOwnedApplicationForUpdate(applicationId, "owner@example.com"))
                .thenThrow(new ApplicationNotFoundException(applicationId));

        assertThrows(
                ApplicationNotFoundException.class,
                () -> service.sign(applicationId, "owner@example.com")
        );
    }

    private ApplicationEntity buildApplication(
            UUID applicationId,
            String status,
            String contractStatus,
            OffsetDateTime contractSignedAt,
            UUID signatureId
    ) {
        OffsetDateTime now = OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC);
        return new ApplicationEntity(
                applicationId,
                status,
                new BigDecimal("500000.00"),
                12,
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "owner@example.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "567890",
                now,
                now,
                "ANNUITY",
                contractStatus,
                contractSignedAt,
                signatureId
        );
    }
}
