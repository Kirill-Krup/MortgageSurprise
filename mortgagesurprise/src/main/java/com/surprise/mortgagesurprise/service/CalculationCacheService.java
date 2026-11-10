package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;

import java.time.Duration;
import java.util.Optional;

public interface CalculationCacheService {
    Optional<CalculationResponse> getFromCache(String calculationHash);
    void putInCache(String calculationHash, CalculationResponse response, Duration ttl);
}
