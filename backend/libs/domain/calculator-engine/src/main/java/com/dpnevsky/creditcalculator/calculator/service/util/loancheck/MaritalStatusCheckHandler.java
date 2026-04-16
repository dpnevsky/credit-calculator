package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import java.math.BigDecimal;
import java.util.List;

import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.DISCOUNT_FOR_MARRIED;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.INCREASE_RATE_FOR_DIVORCED;

public class MaritalStatusCheckHandler implements LoanCheckHandler {

    @Override
    public BigDecimal handle(LoanScoringData scoringData, BigDecimal rate, List<String> rejectionReasons) {
        return switch (scoringData.maritalStatus()) {
            case MARRIED -> rate.subtract(DISCOUNT_FOR_MARRIED);
            case DIVORCED -> rate.add(INCREASE_RATE_FOR_DIVORCED);
            default -> rate;
        };
    }
}
