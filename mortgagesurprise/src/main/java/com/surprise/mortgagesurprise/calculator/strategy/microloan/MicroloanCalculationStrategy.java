package com.surprise.mortgagesurprise.calculator.strategy.microloan;

import com.surprise.mortgagesurprise.calculator.factory.MicroloanCalculationContext;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;
import com.surprise.mortgagesurprise.model.enums.MicroloanType;

public interface MicroloanCalculationStrategy {

    MicroloanType getSupportedType();

    MicroloanCalculationResult calculate(MicroloanCalculationContext context);
}
