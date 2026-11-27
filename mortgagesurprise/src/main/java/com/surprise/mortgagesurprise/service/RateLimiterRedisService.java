package com.surprise.mortgagesurprise.service;

import java.util.UUID;

public interface RateLimiterRedisService {
    boolean allowRequest(UUID userId, String action);
    boolean allowRequest(String clientKey, String action);
}
