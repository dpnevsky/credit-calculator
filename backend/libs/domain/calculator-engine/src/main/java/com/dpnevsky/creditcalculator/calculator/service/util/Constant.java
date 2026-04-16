package com.dpnevsky.creditcalculator.calculator.service.util;

import java.math.BigDecimal;

public final class Constant {

    public static final BigDecimal INITIAL_INSURANCE_PRICE = BigDecimal.ZERO;
    public static final BigDecimal ONE_HUNDRED_PERCENT = BigDecimal.valueOf(100);
    public static final BigDecimal TWELVE_MONTH = BigDecimal.valueOf(12);
    public static final BigDecimal INSURANCE_PRICE_IN_PERCENT = BigDecimal.valueOf(0.04);
    public static final BigDecimal MAX_INSURANCE_PRICE = BigDecimal.valueOf(100_000);
    public static final Integer TOTAL_AMOUNT_AS_FIRST_PAYMENT = 1;
    public static final int FEMALE_MIN_AGE_FOR_DISCOUNT = 32;
    public static final int FEMALE_MAX_AGE_FOR_DISCOUNT = 60;
    public static final int MALE_MIN_AGE_FOR_DISCOUNT = 30;
    public static final int MALE_MAX_AGE_FOR_DISCOUNT = 55;
    public static final BigDecimal DISCOUNT_FOR_FEMALE = BigDecimal.valueOf(3);
    public static final BigDecimal DISCOUNT_FOR_MALE = BigDecimal.valueOf(3);
    public static final BigDecimal INCREASE_RATE_FOR_NON_BINARY = BigDecimal.valueOf(7);
    public static final int MIN_AGE_FOR_CREDIT = 20;
    public static final int MAX_AGE_FOR_CREDIT = 65;
    public static final BigDecimal INCREASE_RATE_FOR_SELF_EMPLOYED = BigDecimal.valueOf(2);
    public static final BigDecimal INCREASE_RATE_FOR_BUSINESS_OWNER = BigDecimal.ONE;
    public static final BigDecimal DISCOUNT_FOR_INSURANCE = BigDecimal.ONE;
    public static final BigDecimal DISCOUNT_FOR_SALARY_CLIENT = BigDecimal.valueOf(3);
    public static final BigDecimal DISCOUNT_FOR_MARRIED = BigDecimal.valueOf(3);
    public static final BigDecimal INCREASE_RATE_FOR_DIVORCED = BigDecimal.ONE;
    public static final BigDecimal NUMBER_OF_SALARY = BigDecimal.valueOf(24);
    public static final int MIN_TOTAL_EXPERIENCE_FOR_CREDIT_IN_MONTH = 18;
    public static final int MIN_CURRENT_EXPERIENCE_FOR_CREDIT_IN_MONTH = 3;
    public static final BigDecimal MIN_FINAL_RATE = BigDecimal.ONE;
    public static final BigDecimal MID_MANAGER_DISCOUNT = BigDecimal.valueOf(2);
    public static final BigDecimal TOP_MANAGER_DISCOUNT = BigDecimal.valueOf(3);

    private Constant() {
    }
}
