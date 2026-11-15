package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.calculator.CalculationContext;
import com.surprise.mortgagesurprise.calculator.CalculationRequestValidator;
import com.surprise.mortgagesurprise.calculator.EarlyRepaymentProcessor;
import com.surprise.mortgagesurprise.calculator.strategy.AnnuityPaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.DifferentiatedPaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.PaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.PaymentCalculationStrategyRegistry;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Single responsibility: Orchestrate mortgage calculation workflow.
 * Validates inputs, prepares calculation context, and delegates to the appropriate PaymentCalculationStrategy.
 */
@Component
public class MortgageCalculatorImpl implements MortgageCalculator {

    public static final RoundingMode CURRENCY_ROUNDING = RoundingMode.HALF_UP;
    public static final int CURRENCY_SCALE = 2;
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private static final BigDecimal TWELVE_HUNDRED = new BigDecimal("1200");

    private final CalculationRequestValidator validator;
    private final PaymentCalculationStrategyRegistry strategyRegistry;

    /**
     * Default constructor for standalone use without Spring (e.g. unit tests).
     */
    public MortgageCalculatorImpl() {
        this.validator = new CalculationRequestValidator();
        EarlyRepaymentProcessor earlyRepaymentProcessor = new EarlyRepaymentProcessor();
        this.strategyRegistry = new PaymentCalculationStrategyRegistry(List.of(
                new AnnuityPaymentCalculationStrategy(earlyRepaymentProcessor),
                new DifferentiatedPaymentCalculationStrategy(earlyRepaymentProcessor)
        ));
    }

    @Autowired
    public MortgageCalculatorImpl(CalculationRequestValidator validator,
                                  PaymentCalculationStrategyRegistry strategyRegistry) {
        this.validator = validator;
        this.strategyRegistry = strategyRegistry;
    }

    @Override
    public CalculationResult calculate(CalculationRequest request) {
        validator.validate(request);

        BigDecimal propertyPrice = request.propertyPrice().setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        BigDecimal downPayment = request.downPayment().setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        BigDecimal loanAmount = propertyPrice.subtract(downPayment).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
        int initialTermMonths = request.termMonths();
        BigDecimal annualRate = request.annualInterestRate();
        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now();

        BigDecimal monthlyRate = annualRate.divide(TWELVE_HUNDRED, MC);

        CalculationContext context = new CalculationContext(
                request,
                propertyPrice,
                downPayment,
                loanAmount,
                initialTermMonths,
                annualRate,
                monthlyRate,
                startDate
        );

        PaymentCalculationStrategy strategy = strategyRegistry.getStrategy(request.paymentType());
        return strategy.calculate(context);
    }
}
