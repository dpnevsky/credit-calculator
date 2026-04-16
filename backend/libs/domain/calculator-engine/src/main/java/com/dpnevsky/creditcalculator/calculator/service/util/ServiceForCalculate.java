package com.dpnevsky.creditcalculator.calculator.service.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.IntStream;

import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.INSURANCE_PRICE_IN_PERCENT;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.MAX_INSURANCE_PRICE;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.ONE_HUNDRED_PERCENT;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.TOTAL_AMOUNT_AS_FIRST_PAYMENT;
import static com.dpnevsky.creditcalculator.calculator.service.util.Constant.TWELVE_MONTH;
import static org.apache.poi.ss.formula.functions.Irr.irr;

public final class ServiceForCalculate {

    private ServiceForCalculate() {
    }

    /**
     * Рассчитывает аннуитетный платёж на основе суммы кредита, процентной ставки и срока.
     * Формула расчета аннуитетного платежа X = S * K
     * где X — аннуитетный платёж, S — сумма кредита, K — коэффициент аннуитета.
     * Коэффициент аннуитета считается:
     * K = (M * (1 + M) ^ S) / ((1 + M) ^ S - 1)
     * где M — месячная процентная ставка по кредиту, S — срок кредита в месяцах.
     *
     * @param totalAmount общая сумма кредита
     * @param rate        годовая процентная ставка
     * @param term        срок кредита в месяцах
     * @return аннуитетный платёж
     */
    public static BigDecimal calculateMonthlyPayment(BigDecimal totalAmount, BigDecimal rate, Integer term) {
        BigDecimal monthlyRate = rate.divide(ONE_HUNDRED_PERCENT.multiply(TWELVE_MONTH), 10, RoundingMode.HALF_EVEN);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return totalAmount.divide(BigDecimal.valueOf(term), 0, RoundingMode.HALF_EVEN);
        }

        BigDecimal pow = BigDecimal.ONE.add(monthlyRate).pow(term);
        BigDecimal annuityCoefficient = monthlyRate.multiply(pow)
                .divide(pow.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_EVEN);

        return totalAmount.multiply(annuityCoefficient).setScale(0, RoundingMode.HALF_EVEN);
    }

    /**
     * Рассчитывает стоимость страховки.
     * Стоимость страховки равняется 4%, но не больше 100_000.
     *
     * @param amount запрашиваемая сумма кредита
     * @return стоимость страховки
     */
    public static BigDecimal calculateInsurancePrice(BigDecimal amount) {
        BigDecimal insurancePrice = amount.multiply(INSURANCE_PRICE_IN_PERCENT);
        if (insurancePrice.compareTo(MAX_INSURANCE_PRICE) > 0) {
            insurancePrice = MAX_INSURANCE_PRICE;
        }
        return insurancePrice;
    }

    /**
     * В соответствии со статьёй 6 Федерального закона «О потребительском кредите (займе)» 353-ФЗ
     * Полная стоимость потребительского кредита (займа) определяется в процентах годовых по формуле:
     * ПСК = i x ЧБП x 100, где ПСК — полная стоимость кредита в процентах годовых с точностью до
     * третьего знака после запятой;
     * ЧБП — число базовых периодов в календарном году. Продолжительность календарного года признаётся
     * равной трёмстам шестидесяти пяти дням;
     * i — процентная ставка базового периода, выраженная в десятичной форме.
     *
     * @param totalAmount    общая сумма кредита
     * @param monthlyPayment ежемесячный платёж
     * @param term           срок кредита в месяцах
     * @param rate           годовая процентная ставка
     * @return полная стоимость кредита
     */
    public static BigDecimal calculatePSK(
            BigDecimal totalAmount,
            BigDecimal monthlyPayment,
            Integer term,
            BigDecimal rate
    ) {
        double[] cashFlows = new double[term + TOTAL_AMOUNT_AS_FIRST_PAYMENT];
        cashFlows[0] = totalAmount.multiply(BigDecimal.valueOf(-1)).doubleValue();
        IntStream.range(0, term)
                .forEach(index -> cashFlows[index + TOTAL_AMOUNT_AS_FIRST_PAYMENT] = monthlyPayment.doubleValue());

        return calculatePskByCashFlows(cashFlows, rate);
    }

    /**
     * Рассчитывает ПСК по фактическому набору платежей.
     * Такой вариант нужен для дифференцированного графика, в котором ежемесячный платёж меняется.
     *
     * @param totalAmount общая сумма кредита
     * @param payments    список платежей по месяцам
     * @param rate        годовая процентная ставка
     * @return полная стоимость кредита
     */
    public static BigDecimal calculatePSK(
            BigDecimal totalAmount,
            List<BigDecimal> payments,
            BigDecimal rate
    ) {
        double[] cashFlows = new double[payments.size() + TOTAL_AMOUNT_AS_FIRST_PAYMENT];
        cashFlows[0] = totalAmount.multiply(BigDecimal.valueOf(-1)).doubleValue();
        IntStream.range(0, payments.size())
                .forEach(index -> cashFlows[index + TOTAL_AMOUNT_AS_FIRST_PAYMENT] = payments.get(index).doubleValue());

        return calculatePskByCashFlows(cashFlows, rate);
    }

    private static BigDecimal calculatePskByCashFlows(double[] cashFlows, BigDecimal fallbackRate) {
        try {
            BigDecimal basePeriodInterestRate = BigDecimal.valueOf(irr(cashFlows));
            return basePeriodInterestRate
                    .multiply(ONE_HUNDRED_PERCENT.multiply(TWELVE_MONTH))
                    .setScale(3, RoundingMode.HALF_UP);
        } catch (RuntimeException exception) {
            return fallbackRate.setScale(3, RoundingMode.HALF_UP);
        }
    }
}
