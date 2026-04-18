package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.NUMBER_OF_SALARY;

public class SalaryAndAmountCheckHandler implements LoanCheckHandler {

    private static final BigDecimal DEPENDENT_SALARY_FACTOR = BigDecimal.valueOf(0.75);

    @Override
    public BigDecimal handle(LoanScoringData scoringData, BigDecimal rate, List<String> rejectionReasons) {
        BigDecimal salary = scoringData.employment().salary();
        BigDecimal amount = scoringData.amount();
        BigDecimal dependentAmount = BigDecimal.valueOf(scoringData.dependentAmount() == null ? 0 : scoringData.dependentAmount());
        BigDecimal salaryDivider = BigDecimal.ONE.add(dependentAmount.multiply(DEPENDENT_SALARY_FACTOR));
        BigDecimal effectiveSalary = salary.divide(salaryDivider, 10, RoundingMode.HALF_UP);

        if (amount.compareTo(effectiveSalary.multiply(NUMBER_OF_SALARY)) > 0) {
            rejectionReasons.add("AMOUNT_EXCEEDS_24_MONTHS_OF_SALARY");
        }

        return rate;
    }
}
