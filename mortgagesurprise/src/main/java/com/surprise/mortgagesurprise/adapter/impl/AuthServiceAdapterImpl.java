package com.surprise.mortgagesurprise.adapter.impl;

import com.surprise.mortgagesurprise.adapter.AuthServiceAdapter;
import com.surprise.mortgagesurprise.dto.auth.AuthResponse;
import com.surprise.mortgagesurprise.dto.auth.RefreshTokenRequest;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.AuthService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceAdapterImpl implements AuthServiceAdapter {

    private final AuthService authService;

    public AuthServiceAdapterImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request, String refreshTokenCookie) {
        var token = isExist(request) ? request.refreshToken() : refreshTokenCookie;
        if (token == null || token.isBlank()) {
            throw new BusinessException("Refresh token is required");
        }
        return authService.refreshToken(new RefreshTokenRequest(token));
    }

    @Override
    public AuthResponse getCurrentUser(CustomUserDetails userDetails) {
        String userRole = "ROLE_USER";
        var role = userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse(userRole);
        return new AuthResponse(
                null,
                null,
                "Bearer",
                0L,
                userDetails.getId(),
                userDetails.getEmail(),
                role,
                userDetails.isEnabled()
        );
    }

    @Override
    public void logout(CustomUserDetails userDetails, RefreshTokenRequest request, String refreshTokenCookie) {
        var token = isExist(request) ? request.refreshToken() : refreshTokenCookie;
        if (userDetails != null && token != null) {
            authService.logout(userDetails.getId(), token);
        }
    }

    private boolean isExist(RefreshTokenRequest request) {
        return (request != null && request.refreshToken() != null && !request.refreshToken().isBlank());
    }
}
