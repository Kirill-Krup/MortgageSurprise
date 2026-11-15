package com.surprise.mortgagesurprise.calculator.strategy;

import com.surprise.mortgagesurprise.calculator.CalculationContext;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.model.enums.PaymentType;

/**
 * Strategy interface for specific mortgage calculation types (e.g. Annuity, Differentiated).
 * Enables extending the calculator with new loan schemes without modifying existing calculation logic (Open-Closed Principle).
 */
public interface PaymentCalculationStrategy {

    PaymentType getSupportedPaymentType();

    CalculationResult calculate(CalculationContext context);
}
