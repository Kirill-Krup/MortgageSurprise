package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;

public interface MicroloanCalculator {

    MicroloanCalculationResult calculate(MicroloanCalculationRequest request);
}
