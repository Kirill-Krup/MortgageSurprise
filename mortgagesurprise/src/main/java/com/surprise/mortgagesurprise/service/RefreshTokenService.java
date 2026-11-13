package com.surprise.mortgagesurprise.service;

import java.time.Duration;
import java.util.UUID;

public interface RefreshTokenService {
    void storeRefreshToken(UUID userId, String token, Duration duration);
    boolean consumeRefreshToken(UUID userId, String token);
}