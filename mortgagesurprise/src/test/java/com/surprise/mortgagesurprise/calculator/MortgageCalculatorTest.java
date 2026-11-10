package com.surprise.mortgagesurprise.calculator;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.dto.calculator.EarlyRepaymentRequest;
import com.surprise.mortgagesurprise.model.enums.EarlyRepaymentType;
import com.surprise.mortgagesurprise.model.enums.PaymentType;
import com.surprise.mortgagesurprise.model.enums.RepaymentStrategy;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import com.surprise.mortgagesurprise.service.impl.MortgageCalculatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MortgageCalculatorTest {

    private MortgageCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MortgageCalculatorImpl();
    }

    @Test
    @DisplayName("Annuity mortgage standard calculation matching bank formula")
    void testStandardAnnuityCalculation() {
        // Loan: 4,000,000 RUB, 240 months (20 years), 10.0% annual rate
        CalculationRequest request = new CalculationRequest(
                new BigDecimal("5000000.00"),
                new BigDecimal("1000000.00"),
                240,
                new BigDecimal("10.00"),
                PaymentType.ANNUITY,
                LocalDate.of(2026, 1, 1),
                List.of()
        );

        CalculationResult result = calculator.calculate(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("4000000.00"), result.loanAmount());
        assertEquals(240, result.actualTermMonths());

        // Bank standard monthly annuity payment: 38,600.87 RUB
        assertEquals(new BigDecimal("38600.87"), result.monthlyPayment());

        // Closing balance at the end must be exactly 0.00
        var lastItem = result.schedule().getLast();
        assertEquals(new BigDecimal("0.00"), lastItem.closingBalance());

        // Check overpayment (total interest) around 5,264,216
        assertTrue(result.totalInterest().compareTo(new BigDecimal("5200000")) > 0);
        assertTrue(result.totalInterest().compareTo(new BigDecimal("5300000")) < 0);
        assertEquals(result.loanAmount().add(result.totalInterest()), result.totalPayment());
    }

    @Test
    @DisplayName("Differentiated mortgage calculation with declining payments")
    void testStandardDifferentiatedCalculation() {
        // Loan: 1,200,000 RUB, 12 months, 12.0% annual rate
        CalculationRequest request = new CalculationRequest(
                new BigDecimal("1500000.00"),
                new BigDecimal("300000.00"),
                12,
                new BigDecimal("12.00"),
                PaymentType.DIFFERENTIATED,
                LocalDate.of(2026, 1, 1),
                List.of()
        );

        CalculationResult result = calculator.calculate(request);

        assertNotNull(result);
        assertEquals(12, result.actualTermMonths());
        // Principal part is 100,000 each month
        // First month interest: 1,200,000 * 0.01 = 12,000 -> payment = 112,000
        assertEquals(new BigDecimal("112000.00"), result.schedule().getFirst().totalMonthlyPayment());
        // Last month interest: 100,000 * 0.01 = 1,000 -> payment = 101,000
        assertEquals(new BigDecimal("101000.00"), result.schedule().getLast().totalMonthlyPayment());
        // Schedule closing balance ends at 0.00
        assertEquals(new BigDecimal("0.00"), result.schedule().getLast().closingBalance());

        // Total interest = (12 + 11 + ... + 1) * 1000 = 78,000
        assertEquals(new BigDecimal("78000.00"), result.totalInterest());
    }

    @Test
    @DisplayName("Early repayment with REDUCE_TERM reduces total loan duration")
    void testEarlyRepaymentReduceTerm() {
        CalculationRequest request = new CalculationRequest(
                new BigDecimal("1200000.00"),
                new BigDecimal("200000.00"),
                120,
                new BigDecimal("12.00"),
                PaymentType.ANNUITY,
                LocalDate.of(2026, 1, 1),
                List.of(
                        new EarlyRepaymentRequest(
                                6,
                                null,
                                new BigDecimal("300000.00"),
                                RepaymentStrategy.REDUCE_TERM,
                                EarlyRepaymentType.ONE_TIME
                        )
                )
        );

        CalculationResult resultWithEarly = calculator.calculate(request);

        assertTrue(resultWithEarly.actualTermMonths() < 120, "Actual term should be less than 120 months");
        assertEquals(new BigDecimal("0.00"), resultWithEarly.schedule().getLast().closingBalance());
    }

    @Test
    @DisplayName("Early repayment with REDUCE_PAYMENT lowers future monthly payments")
    void testEarlyRepaymentReducePayment() {
        CalculationRequest baselineRequest = new CalculationRequest(
                new BigDecimal("1200000.00"),
                new BigDecimal("200000.00"),
                120,
                new BigDecimal("12.00"),
                PaymentType.ANNUITY,
                LocalDate.of(2026, 1, 1),
                List.of()
        );
        CalculationResult baseline = calculator.calculate(baselineRequest);

        CalculationRequest earlyRequest = new CalculationRequest(
                new BigDecimal("1200000.00"),
                new BigDecimal("200000.00"),
                120,
                new BigDecimal("12.00"),
                PaymentType.ANNUITY,
                LocalDate.of(2026, 1, 1),
                List.of(
                        new EarlyRepaymentRequest(
                                6,
                                null,
                                new BigDecimal("300000.00"),
                                RepaymentStrategy.REDUCE_PAYMENT,
                                EarlyRepaymentType.ONE_TIME
                        )
                )
        );

        CalculationResult withEarly = calculator.calculate(earlyRequest);

        assertEquals(120, withEarly.actualTermMonths());
        BigDecimal paymentMonth5 = withEarly.schedule().get(4).totalMonthlyPayment();
        BigDecimal paymentMonth7 = withEarly.schedule().get(6).totalMonthlyPayment();
        assertTrue(paymentMonth7.compareTo(paymentMonth5) < 0, "Payment in month 7 should be lower than month 5");
        assertTrue(withEarly.totalInterest().compareTo(baseline.totalInterest()) < 0);
    }

    @Test
    @DisplayName("Validation throws IllegalArgumentException when downPayment >= propertyPrice")
    void testInvalidDownPayment() {
        CalculationRequest request = new CalculationRequest(
                new BigDecimal("1000000.00"),
                new BigDecimal("1000000.00"),
                120,
                new BigDecimal("10.00"),
                PaymentType.ANNUITY,
                LocalDate.now(),
                List.of()
        );

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(request));
    }
}
