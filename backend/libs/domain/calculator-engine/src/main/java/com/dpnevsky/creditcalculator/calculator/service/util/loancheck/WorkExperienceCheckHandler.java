package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import java.math.BigDecimal;
import java.util.List;

import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.MIN_CURRENT_EXPERIENCE_FOR_CREDIT_IN_MONTH;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.MIN_TOTAL_EXPERIENCE_FOR_CREDIT_IN_MONTH;

public class WorkExperienceCheckHandler implements LoanCheckHandler {

    @Override
    public BigDecimal handle(LoanScoringData scoringData, BigDecimal rate, List<String> rejectionReasons) {
        int totalExperience = scoringData.employment().workExperienceTotal();
        int currentExperience = scoringData.employment().workExperienceCurrent();

        if (totalExperience < MIN_TOTAL_EXPERIENCE_FOR_CREDIT_IN_MONTH) {
            rejectionReasons.add("TOTAL_WORK_EXPERIENCE_TOO_LOW");
        }
        if (currentExperience < MIN_CURRENT_EXPERIENCE_FOR_CREDIT_IN_MONTH) {
            rejectionReasons.add("CURRENT_WORK_EXPERIENCE_TOO_LOW");
        }

        return rate;
    }
}
