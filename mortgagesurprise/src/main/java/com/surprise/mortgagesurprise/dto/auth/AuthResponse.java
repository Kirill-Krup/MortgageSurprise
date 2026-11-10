package com.surprise.mortgagesurprise.dto.auth;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UUID userId,
        String email,
        String role
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresInMs, UUID userId, String email, String role) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInMs, userId, email, role);
    }
}
