package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import java.math.BigDecimal;
import java.util.List;

import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.MID_MANAGER_DISCOUNT;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.TOP_MANAGER_DISCOUNT;

public class EmploymentPositionCheckHandler implements LoanCheckHandler {

    @Override
    public BigDecimal handle(LoanScoringData scoringData, BigDecimal rate, List<String> rejectionReasons) {
        LoanScoringData.PositionType position = scoringData.employment().position();
        if (position == LoanScoringData.PositionType.MID_MANAGER) {
            return rate.subtract(MID_MANAGER_DISCOUNT);
        }
        if (position == LoanScoringData.PositionType.TOP_MANAGER) {
            return rate.subtract(TOP_MANAGER_DISCOUNT);
        }
        return rate;
    }
}
