package com.dpnevsky.creditcalculator.calculator.service.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceForCalculateTest {
    @Test
    void preservesKopecksForAnnuityPayments() {
        assertEquals(new BigDecimal("8884.88"), ServiceForCalculate.calculateMonthlyPayment(
                new BigDecimal("100000.00"), new BigDecimal("12.00"), 12));
    }

    @Test
    void preservesKopecksForInterestFreePayments() {
        assertEquals(new BigDecimal("33333.33"), ServiceForCalculate.calculateMonthlyPayment(
                new BigDecimal("100000.00"), BigDecimal.ZERO, 3));
    }
}
