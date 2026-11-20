package com.surprise.mortgagesurprise.adapter;

import com.surprise.mortgagesurprise.dto.auth.AuthResponse;
import com.surprise.mortgagesurprise.dto.auth.RefreshTokenRequest;
import com.surprise.mortgagesurprise.security.CustomUserDetails;

public interface AuthAdapter {
    AuthResponse refresh(RefreshTokenRequest request, String refreshTokenCookie);

    AuthResponse getCurrentUser(CustomUserDetails userDetails);

    void logout(CustomUserDetails userDetails, RefreshTokenRequest request, String refreshTokenCookie);
}