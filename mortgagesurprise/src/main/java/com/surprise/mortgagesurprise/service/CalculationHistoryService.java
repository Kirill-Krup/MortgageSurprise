package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;

import java.util.UUID;

/**
 * Single responsibility: Record user mortgage calculation history.
 */
public interface CalculationHistoryService {

    void recordHistory(UUID userId, String scheduleId, CalculationResult result);
}
