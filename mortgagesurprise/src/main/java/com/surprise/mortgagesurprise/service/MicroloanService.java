package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;

import java.util.UUID;

public interface MicroloanService {

    MicroloanCalculationResponse calculateAndPersist(MicroloanCalculationRequest request,
                                                     UUID userId);
}
