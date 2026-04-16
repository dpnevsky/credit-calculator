package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import java.math.BigDecimal;
import java.util.List;

import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.INCREASE_RATE_FOR_BUSINESS_OWNER;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.INCREASE_RATE_FOR_SELF_EMPLOYED;

public class EmploymentStatusCheckHandler implements LoanCheckHandler {

    @Override
    public BigDecimal handle(LoanScoringData scoringData, BigDecimal rate, List<String> rejectionReasons) {
        LoanScoringData.EmploymentStatusType status = scoringData.employment().employmentStatus();
        return switch (status) {
            case UNEMPLOYED -> {
                rejectionReasons.add("UNEMPLOYED");
                yield rate;
            }
            case SELF_EMPLOYED -> rate.add(INCREASE_RATE_FOR_SELF_EMPLOYED);
            case BUSINESS_OWNER -> rate.add(INCREASE_RATE_FOR_BUSINESS_OWNER);
            default -> rate;
        };
    }
}
