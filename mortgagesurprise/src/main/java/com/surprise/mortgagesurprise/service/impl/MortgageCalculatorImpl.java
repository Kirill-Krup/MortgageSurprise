package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.calculator.CalculationContext;
import com.surprise.mortgagesurprise.calculator.factory.CalculationContextFactory;
import com.surprise.mortgagesurprise.calculator.strategy.PaymentCalculationStrategyRegistry;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.service.MortgageCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MortgageCalculatorImpl implements MortgageCalculator {

    private final CalculationContextFactory calculationContextFactory;
    private final PaymentCalculationStrategyRegistry strategyRegistry;

    @Override
    public CalculationResult calculate(CalculationRequest request) {
        CalculationContext calculationContext = calculationContextFactory.createMortgageContext(request);
        return strategyRegistry.getStrategy(request.paymentType()).calculate(calculationContext);
    }
}