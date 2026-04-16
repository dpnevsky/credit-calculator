package com.dpnevsky.creditcalculator.calculator.service.util.loancheck;

import java.math.BigDecimal;
import java.util.List;

public interface LoanCheckHandler {

    BigDecimal handle(LoanScoringData scoringData, BigDecimal rate, List<String> rejectionReasons);
}
