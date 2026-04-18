package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalaryAndAmountCheckHandlerTest {

    private final SalaryAndAmountCheckHandler handler = new SalaryAndAmountCheckHandler();

    @Test
    void approvesBoundaryAmountWithoutDependents() {
        List<String> rejectionReasons = new ArrayList<>();

        handler.handle(buildScoringData("1200000.00", "50000.00", 0), BigDecimal.valueOf(15), rejectionReasons);

        assertTrue(rejectionReasons.isEmpty());
    }

    @Test
    void rejectsWhenDependentsReduceEffectiveSalary() {
        List<String> rejectionReasons = new ArrayList<>();

        handler.handle(buildScoringData("800000.00", "50000.00", 1), BigDecimal.valueOf(15), rejectionReasons);

        assertEquals(List.of("AMOUNT_EXCEEDS_24_MONTHS_OF_SALARY"), rejectionReasons);
    }

    @Test
    void approvesBoundaryAmountForMultipleDependents() {
        List<String> rejectionReasons = new ArrayList<>();

        handler.handle(buildScoringData("480000.00", "50000.00", 2), BigDecimal.valueOf(15), rejectionReasons);

        assertTrue(rejectionReasons.isEmpty());
    }

    private LoanScoringData buildScoringData(String amount, String salary, int dependentAmount) {
        return new LoanScoringData(
                new BigDecimal(amount),
                LocalDate.of(1990, 1, 1),
                LoanScoringData.GenderType.MALE,
                LoanScoringData.MaritalStatusType.SINGLE,
                dependentAmount,
                false,
                false,
                new LoanScoringData.Employment(
                        LoanScoringData.EmploymentStatusType.EMPLOYED,
                        new BigDecimal(salary),
                        LoanScoringData.PositionType.OTHER,
                        24,
                        12
                )
        );
    }
}
