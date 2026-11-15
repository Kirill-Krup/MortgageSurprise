package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;

/**
 * Single responsibility: Manage storage and retrieval of payment schedule documents.
 */
public interface PaymentScheduleService {

    PaymentScheduleDocument saveSchedule(CalculationResult result, String cacheHash);

    PaymentScheduleDocument saveSchedule(CalculationResult result);

    PaymentScheduleDocument getScheduleById(String scheduleId);
}
