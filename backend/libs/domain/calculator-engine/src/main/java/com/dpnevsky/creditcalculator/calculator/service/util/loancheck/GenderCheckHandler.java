package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.DISCOUNT_FOR_FEMALE;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.DISCOUNT_FOR_MALE;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.FEMALE_MAX_AGE_FOR_DISCOUNT;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.FEMALE_MIN_AGE_FOR_DISCOUNT;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.INCREASE_RATE_FOR_NON_BINARY;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.MALE_MAX_AGE_FOR_DISCOUNT;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.MALE_MIN_AGE_FOR_DISCOUNT;

public class GenderCheckHandler implements LoanCheckHandler {

    @Override
    public BigDecimal handle(LoanScoringData scoringData, BigDecimal rate, List<String> rejectionReasons) {
        LoanScoringData.GenderType gender = scoringData.gender();
        int age = LocalDate.now().getYear() - scoringData.birthdate().getYear();

        if (gender == LoanScoringData.GenderType.FEMALE
                && age >= FEMALE_MIN_AGE_FOR_DISCOUNT
                && age <= FEMALE_MAX_AGE_FOR_DISCOUNT) {
            return rate.subtract(DISCOUNT_FOR_FEMALE);
        }
        if (gender == LoanScoringData.GenderType.MALE
                && age >= MALE_MIN_AGE_FOR_DISCOUNT
                && age <= MALE_MAX_AGE_FOR_DISCOUNT) {
            return rate.subtract(DISCOUNT_FOR_MALE);
        }
        if (gender == LoanScoringData.GenderType.NON_BINARY) {
            return rate.add(INCREASE_RATE_FOR_NON_BINARY);
        }
        return rate;
    }
}
