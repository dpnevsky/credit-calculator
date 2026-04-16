package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import java.math.BigDecimal;
import java.util.List;

import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.NUMBER_OF_SALARY;

public class SalaryAndAmountCheckHandler implements LoanCheckHandler {

    @Override
    public BigDecimal handle(LoanScoringData scoringData, BigDecimal rate, List<String> rejectionReasons) {
        BigDecimal salary = scoringData.employment().salary();
        BigDecimal amount = scoringData.amount();

        if (amount.compareTo(salary.multiply(NUMBER_OF_SALARY)) > 0) {
            rejectionReasons.add("AMOUNT_EXCEEDS_24_MONTHS_OF_SALARY");
        }

        return rate;
    }
}
