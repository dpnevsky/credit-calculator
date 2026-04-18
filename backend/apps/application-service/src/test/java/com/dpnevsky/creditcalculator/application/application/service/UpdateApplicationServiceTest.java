package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.UpdateApplicationRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.UpdateApplicationResponse;
import com.dpnevsky.creditcalculator.application.domain.prescoring.LegacyPrescoringService;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.entity.ApplicationEntity;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateApplicationServiceTest {

    @Test
    void returnsNoOffersAfterSuccessfulUpdatePrescoring() {
        LegacyPrescoringService prescoringService = mock(LegacyPrescoringService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        UpdateApplicationService service = new UpdateApplicationService(applicationRepository, prescoringService);
        UUID applicationId = UUID.randomUUID();

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(buildApplication(applicationId, "DRAFT")));
        when(prescoringService.evaluate(any(), any(), any()))
                .thenReturn(new LegacyPrescoringService.PrescoringResult(true, List.of()));

        UpdateApplicationResponse response = service.update(applicationId, buildUpdateRequest());

        assertEquals("DRAFT", response.status());
        assertTrue(response.preliminaryOffers().isEmpty());
        verify(applicationRepository).save(any());
    }

    private UpdateApplicationRequest buildUpdateRequest() {
        return new UpdateApplicationRequest(
                new BigDecimal("700000.00"),
                24,
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "ivan@example.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "567890"
        );
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
}
