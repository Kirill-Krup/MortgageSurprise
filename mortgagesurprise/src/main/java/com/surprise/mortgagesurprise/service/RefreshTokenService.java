package com.surprise.mortgagesurprise.service;

import java.time.Duration;
import java.util.UUID;

public interface RefreshTokenService {
    void storeRefreshToken(UUID userId, String token, Duration duration);
    boolean isRefreshTokenValid(UUID userId, String token);
    void revokeRefreshToken(UUID userId, String token);
}