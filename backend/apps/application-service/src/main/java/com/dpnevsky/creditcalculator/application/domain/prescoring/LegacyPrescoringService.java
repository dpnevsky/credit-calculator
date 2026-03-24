package com.dpnevsky.creditcalculator.application.domain.prescoring;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Service
public class LegacyPrescoringService {

    private static final BigDecimal MIN_AMOUNT = BigDecimal.valueOf(20_000);
    private static final int MIN_TERM_MONTHS = 6;
    private static final int MIN_AGE_YEARS = 18;

    public PrescoringResult evaluate(
            BigDecimal amount,
            Integer termMonths,
            LocalDate birthDate
    ) {
        List<String> reasons = new ArrayList<>();

        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0) {
            reasons.add("AMOUNT_BELOW_MINIMUM");
        }

        if (termMonths == null || termMonths < MIN_TERM_MONTHS) {
            reasons.add("TERM_BELOW_MINIMUM");
        }

        if (birthDate == null || calculateAge(birthDate, LocalDate.now()) < MIN_AGE_YEARS) {
            reasons.add("AGE_BELOW_MINIMUM");
        }

        return new PrescoringResult(
                reasons.isEmpty(),
                reasons
        );
    }

    private int calculateAge(LocalDate birthDate, LocalDate currentDate) {
        return Period.between(birthDate, currentDate).getYears();
    }

    public record PrescoringResult(
            boolean passed,
            List<String> reasons
    ) {
    }
}