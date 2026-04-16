package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.MAX_AGE_FOR_CREDIT;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.MIN_AGE_FOR_CREDIT;

public class AgeCheckHandler implements LoanCheckHandler {

    @Override
    public BigDecimal handle(LoanScoringData scoringData, BigDecimal rate, List<String> rejectionReasons) {
        int age = LocalDate.now().getYear() - scoringData.birthdate().getYear();
        if (age < MIN_AGE_FOR_CREDIT || age > MAX_AGE_FOR_CREDIT) {
            rejectionReasons.add("AGE_OUT_OF_RANGE");
        }
        return rate;
    }
}
