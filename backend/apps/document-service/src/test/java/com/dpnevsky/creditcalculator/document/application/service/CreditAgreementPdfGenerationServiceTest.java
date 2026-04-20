package com.dpnevsky.creditcalculator.document.application.service;

import com.dpnevsky.creditcalculator.contracts.document.events.CreditAgreementRenderData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditAgreementPdfGenerationServiceTest {

    @Test
    void generatesPdfUsingBundledFontResource() {
        CreditAgreementPdfGenerationService service = new CreditAgreementPdfGenerationService(
                new BundledPdfFontProvider()
        );

        byte[] pdfBytes = service.generate(
                new CreditAgreementRenderData(
                        "Иван",
                        "Иванов",
                        "Иванович",
                        "1234",
                        "567890",
                        new BigDecimal("540000.00"),
                        12,
                        new BigDecimal("12.00"),
                        "ANNUITY"
                ),
                OffsetDateTime.of(2026, 4, 17, 12, 0, 0, 0, ZoneOffset.UTC)
        );

        assertTrue(pdfBytes.length > 4);
        assertTrue(Arrays.equals(new byte[] {'%', 'P', 'D', 'F'}, Arrays.copyOf(pdfBytes, 4)));
    }
}
