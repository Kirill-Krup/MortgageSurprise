package com.surprise.mortgagesurprise.adapter;

import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationRequest;
import com.surprise.mortgagesurprise.dto.audit.MicroloanApplicationResponse;
import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.security.CustomUserDetails;

public interface CalculatorAdapter {
    CalculationResponse calculate(CalculationRequest request, CustomUserDetails userDetails);

    MicroloanCalculationResponse calculateMicroloan(MicroloanCalculationRequest request,
                                                    CustomUserDetails userDetails);

    MicroloanApplicationResponse applyMicroloan(MicroloanApplicationRequest request,
                                                CustomUserDetails userDetails);
}