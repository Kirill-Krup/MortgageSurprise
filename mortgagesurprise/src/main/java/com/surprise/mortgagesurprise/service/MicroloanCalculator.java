package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;

public interface MicroloanCalculator extends Calculator<MicroloanCalculationRequest,MicroloanCalculationResult> {

    MicroloanCalculationResult calculate(MicroloanCalculationRequest request);
}
