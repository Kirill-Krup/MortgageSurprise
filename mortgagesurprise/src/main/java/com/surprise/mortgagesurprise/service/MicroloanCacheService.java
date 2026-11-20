package com.surprise.mortgagesurprise.service;

import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;

import java.time.Duration;
import java.util.Optional;

public interface MicroloanCacheService {
    Optional<MicroloanCalculationResponse> getFromCache(String calculationHash);
    void putInCache(String calculationHash, MicroloanCalculationResponse response);
    void putInCache(String calculationHash, MicroloanCalculationResponse response, Duration ttl);
}
