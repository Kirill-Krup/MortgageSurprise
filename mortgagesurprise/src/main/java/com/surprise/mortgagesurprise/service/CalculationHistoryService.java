package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.CalculationResult;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResult;

import java.util.UUID;

/**
 * Single responsibility: Record user calculation history for loan products.
 */
public interface CalculationHistoryService {

    void recordHistory(UUID userId, String scheduleId, CalculationResult result);

    void recordHistory(UUID userId, String scheduleId, MicroloanCalculationResult result);
}
