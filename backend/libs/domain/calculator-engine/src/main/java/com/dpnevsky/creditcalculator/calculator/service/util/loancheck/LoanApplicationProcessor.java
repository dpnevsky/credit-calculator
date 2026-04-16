package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.MIN_FINAL_RATE;

public class LoanApplicationProcessor {

    private final List<LoanCheckHandler> handlers;

    public LoanApplicationProcessor(List<LoanCheckHandler> handlers) {
        this.handlers = handlers;
    }

    public LoanApplicationProcessingResult processLoanApplication(LoanScoringData scoringData, BigDecimal baseRate) {
        BigDecimal currentRate = baseRate;
        List<String> rejectionReasons = new ArrayList<>();

        for (LoanCheckHandler handler : handlers) {
            currentRate = handler.handle(scoringData, currentRate, rejectionReasons);
        }

        return new LoanApplicationProcessingResult(finalCheck(currentRate), List.copyOf(rejectionReasons));
    }

    private BigDecimal finalCheck(BigDecimal rate) {
        if (rate.compareTo(MIN_FINAL_RATE) < 0) {
            return MIN_FINAL_RATE;
        }
        return rate;
    }

    public record LoanApplicationProcessingResult(
            BigDecimal rate,
            List<String> rejectionReasons
    ) {
    }
}
