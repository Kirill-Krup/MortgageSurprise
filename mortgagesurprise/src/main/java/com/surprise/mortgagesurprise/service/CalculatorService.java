package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.CalculationRequest;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;

import java.util.UUID;

public interface CalculatorService {
    CalculationResponse calculateAndPersist(CalculationRequest request, UUID userId);
    PaymentScheduleDocument getScheduleById(String scheduleId);
}
