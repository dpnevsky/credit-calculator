package com.dpnevsky.creditcalculator.application.domain.offers;

import com.dpnevsky.creditcalculator.calculator.service.util.ServiceForCalculate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class LegacyPreOfferGenerationService {

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
        return ServiceForCalculate.calculateInsurancePrice(requestedAmount)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal totalAmount, BigDecimal rate, Integer termMonths) {
        return ServiceForCalculate.calculateMonthlyPayment(totalAmount, rate, termMonths)
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
