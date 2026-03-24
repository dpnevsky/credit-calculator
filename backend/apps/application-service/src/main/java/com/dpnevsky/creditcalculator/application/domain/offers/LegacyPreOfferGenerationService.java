package com.dpnevsky.creditcalculator.application.domain.offers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class LegacyPreOfferGenerationService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal INSURANCE_PERCENT = BigDecimal.valueOf(0.04);
    private static final BigDecimal MAX_INSURANCE_PRICE = BigDecimal.valueOf(100_000);

    @Value("${loan.base-rate:15.00}")
    private BigDecimal baseRate;

    public List<PreliminaryOffer> generateOffers(UUID applicationId, BigDecimal requestedAmount, Integer termMonths) {
        return List.of(
                buildOffer(applicationId, requestedAmount, termMonths, false, false),
                buildOffer(applicationId, requestedAmount, termMonths, true, false),
                buildOffer(applicationId, requestedAmount, termMonths, false, true),
                buildOffer(applicationId, requestedAmount, termMonths, true, true)
        );
    }

    private PreliminaryOffer buildOffer(
            UUID applicationId,
            BigDecimal requestedAmount,
            Integer termMonths,
            boolean insuranceEnabled,
            boolean salaryClient
    ) {
        BigDecimal rate = calculateRate(baseRate, insuranceEnabled, salaryClient);
        BigDecimal insurancePrice = insuranceEnabled ? calculateInsurancePrice(requestedAmount) : BigDecimal.ZERO;
        BigDecimal totalAmount = requestedAmount.add(insurancePrice);
        BigDecimal monthlyPayment = calculateMonthlyPayment(totalAmount, rate, termMonths);

        return new PreliminaryOffer(
                applicationId,
                requestedAmount,
                totalAmount,
                termMonths,
                monthlyPayment,
                rate,
                insuranceEnabled,
                salaryClient
        );
    }

    private BigDecimal calculateRate(BigDecimal baseRate, boolean insuranceEnabled, boolean salaryClient) {
        BigDecimal rate = baseRate;

        if (insuranceEnabled) {
            rate = rate.subtract(BigDecimal.ONE);
        }

        if (salaryClient) {
            rate = rate.subtract(BigDecimal.valueOf(3));
        }

        return rate.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateInsurancePrice(BigDecimal requestedAmount) {
        BigDecimal insurancePrice = requestedAmount.multiply(INSURANCE_PERCENT);

        if (insurancePrice.compareTo(MAX_INSURANCE_PRICE) > 0) {
            insurancePrice = MAX_INSURANCE_PRICE;
        }

        return insurancePrice.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal totalAmount, BigDecimal rate, Integer termMonths) {
        BigDecimal monthlyRate = rate.divide(ONE_HUNDRED.multiply(TWELVE), 10, RoundingMode.HALF_EVEN);
        BigDecimal pow = BigDecimal.ONE.add(monthlyRate).pow(termMonths);
        BigDecimal annuityCoefficient = monthlyRate.multiply(pow)
                .divide(pow.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_EVEN);

        return totalAmount.multiply(annuityCoefficient)
                .setScale(0, RoundingMode.HALF_EVEN);
    }

    public record PreliminaryOffer(
            UUID applicationId,
            BigDecimal requestedAmount,
            BigDecimal totalAmount,
            Integer termMonths,
            BigDecimal monthlyPayment,
            BigDecimal rate,
            boolean insuranceEnabled,
            boolean salaryClient
    ) {
    }
}