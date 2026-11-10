package com.surprise.mortgagesurprise.service;

public interface RateLimiterRedisService {
    boolean allowRequest(String clientIdentifier, String action);
}
