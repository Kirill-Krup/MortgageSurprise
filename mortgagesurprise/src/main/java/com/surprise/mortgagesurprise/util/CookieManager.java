package com.surprise.mortgagesurprise.util;

import com.surprise.mortgagesurprise.dto.auth.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class CookieManager {

    public static void setAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
        if (authResponse.accessToken() != null) {
            var accessCookie = ResponseCookie.from("accessToken", authResponse.accessToken())
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ofMinutes(15))
                    .sameSite("Lax")
                    .secure(false)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        }
        if (authResponse.refreshToken() != null) {
            var refreshCookie = ResponseCookie.from("refreshToken", authResponse.refreshToken())
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ofDays(7))
                    .sameSite("Lax")
                    .secure(false)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }
    }

    public static void clearAuthCookies(HttpServletResponse response) {
        var accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .secure(false)
                .build();
        var refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .secure(false)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private CookieManager() {
    }
}
