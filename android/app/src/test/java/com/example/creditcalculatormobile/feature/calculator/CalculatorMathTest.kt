package com.example.creditcalculatormobile.feature.calculator

import com.example.creditcalculatormobile.feature.calculator.model.CalculatorPaymentType
import org.junit.Assert.*
import org.junit.Test

class CalculatorMathTest {
    @Test fun `NaN infinity and excessive terms are rejected`() {
        for (value in listOf("NaN", "Infinity", "1e309", "1e308")) {
            assertTrue(parseCalculatorInput(value, "12", "10", CalculatorPaymentType.ANNUITY).isFailure)
            assertTrue(parseCalculatorInput("1000", "12", value, CalculatorPaymentType.ANNUITY).isFailure)
        }
        assertTrue(parseCalculatorInput("1000", "361", "10", CalculatorPaymentType.ANNUITY).isFailure)
    }

    @Test fun `zero rate amortizes the full principal to the kopeck`() {
        for (type in CalculatorPaymentType.entries) {
            val result = calculateLoan(ParsedCalculatorInput(100.0, 3, 0.0, type))
            assertEquals(100.0, result.schedule.sumOf { it.principal }, 0.001)
            assertEquals(100.0, result.totalPayment, 0.001)
            assertEquals(0.0, result.schedule.last().remainingBalance, 0.001)
            assertEquals(33.34, result.lastPayment!!, 0.001)
        }
    }

    @Test fun `small nonzero rate produces finite payments`() {
        val result = calculateLoan(ParsedCalculatorInput(100000.0, 12, 1e-12, CalculatorPaymentType.ANNUITY))
        assertTrue(result.totalPayment.isFinite())
        assertEquals(100000.0, result.schedule.sumOf { it.principal }, 0.01)
    }

    @Test fun `schedule totals match displayed totals for both types`() {
        for (type in CalculatorPaymentType.entries) {
            val result = calculateLoan(ParsedCalculatorInput(500000.0, 36, 15.0, type))
            assertEquals(500000.0, result.schedule.sumOf { it.principal }, 0.01)
            assertEquals(result.schedule.sumOf { it.payment }, result.totalPayment, 0.01)
            assertEquals(result.schedule.last().payment, result.lastPayment!!, 0.001)
            assertEquals(0.0, result.schedule.last().remainingBalance, 0.001)
        }
    }
}
