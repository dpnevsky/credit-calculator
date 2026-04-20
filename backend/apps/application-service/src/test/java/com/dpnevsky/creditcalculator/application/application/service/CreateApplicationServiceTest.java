package com.dpnevsky.creditcalculator.application.application.service;

import com.dpnevsky.creditcalculator.application.api.rest.dto.CreateApplicationRequest;
import com.dpnevsky.creditcalculator.application.api.rest.dto.CreateApplicationResponse;
import com.dpnevsky.creditcalculator.application.domain.prescoring.LegacyPrescoringService;
import com.dpnevsky.creditcalculator.application.infrastructure.persistence.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateApplicationServiceTest {

    @Test
    void returnsNoOffersAfterSuccessfulPrescoring() {
        LegacyPrescoringService prescoringService = mock(LegacyPrescoringService.class);
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        CreateApplicationService service = new CreateApplicationService(prescoringService, applicationRepository);

        when(prescoringService.evaluate(any(), any(), any()))
                .thenReturn(new LegacyPrescoringService.PrescoringResult(true, List.of()));

        CreateApplicationResponse response = service.create(buildCreateRequest(), "owner@example.com");

        assertEquals("DRAFT", response.status());
        assertTrue(response.preliminaryOffers().isEmpty());
        verify(applicationRepository).save(argThat(entity -> "owner@example.com".equals(entity.getEmail())));
    }

    private CreateApplicationRequest buildCreateRequest() {
        return new CreateApplicationRequest(
                new BigDecimal("500000.00"),
                12,
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "ivan@example.com",
                LocalDate.of(1990, 1, 1),
                "1234",
                "567890"
        );
    }
}
