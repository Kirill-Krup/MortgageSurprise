package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.calculator.CalculationContext;
import com.surprise.mortgagesurprise.calculator.CalculationRequestValidator;
import com.surprise.mortgagesurprise.calculator.EarlyRepaymentProcessor;
import com.surprise.mortgagesurprise.calculator.factory.CalculationContextFactory;
import com.surprise.mortgagesurprise.calculator.strategy.AnnuityPaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.DifferentiatedPaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.PaymentCalculationStrategy;
import com.surprise.mortgagesurprise.calculator.strategy.PaymentCalculationStrategyRegistry;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import com.surprise.mortgagesurprise.model.enums.LoanProductType;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class MortgageCalculatorImpl implements MortgageCalculator {

    private final CalculationContextFactory  calculationContextFactory;
    private final PaymentCalculationStrategyRegistry strategyRegistry;

    @Override
    public CalculationResult calculate(CalculationRequest request) {
        CalculationContext calculationContext = calculationContextFactory.createMortgageContext(request);
        return strategyRegistry.getStrategy(request.paymentType()).calculate(calculationContext);
    }
}
