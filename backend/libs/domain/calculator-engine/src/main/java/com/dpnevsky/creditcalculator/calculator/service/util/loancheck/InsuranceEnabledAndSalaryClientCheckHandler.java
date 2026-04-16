package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import java.math.BigDecimal;
import java.util.List;

import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.DISCOUNT_FOR_INSURANCE;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.DISCOUNT_FOR_SALARY_CLIENT;

public class InsuranceEnabledAndSalaryClientCheckHandler implements LoanCheckHandler {

    @Override
    public BigDecimal handle(LoanScoringData scoringData, BigDecimal rate, List<String> rejectionReasons) {
        BigDecimal currentRate = rate;
        if (scoringData.insuranceEnabled()) {
            currentRate = currentRate.subtract(DISCOUNT_FOR_INSURANCE);
        }
        if (scoringData.salaryClient()) {
            currentRate = currentRate.subtract(DISCOUNT_FOR_SALARY_CLIENT);
        }
        return currentRate;
    }
}
