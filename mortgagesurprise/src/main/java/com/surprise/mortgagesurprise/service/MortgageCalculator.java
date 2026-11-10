package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;

public interface MortgageCalculator {
    CalculationResult calculate(CalculationRequest request);
}
