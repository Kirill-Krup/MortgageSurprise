package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.auth.AuthResponse;
import com.surprise.mortgagesurprise.dto.auth.LoginRequest;
import com.surprise.mortgagesurprise.dto.auth.MagicLinkRequest;
import com.surprise.mortgagesurprise.dto.auth.MagicLinkResponse;
import com.surprise.mortgagesurprise.dto.auth.MagicLinkVerifyRequest;
import com.surprise.mortgagesurprise.dto.auth.OAuth2CodeRequest;
import com.surprise.mortgagesurprise.dto.auth.RefreshTokenRequest;
import com.surprise.mortgagesurprise.dto.auth.RegisterRequest;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.AuthService;
import com.surprise.mortgagesurprise.service.impl.MagicLinkService;
import com.surprise.mortgagesurprise.service.impl.OAuth2CodeStore;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final MagicLinkService magicLinkService;
    private final OAuth2CodeStore oAuth2CodeStore;

    public AuthController(AuthService authService,
                          MagicLinkService magicLinkService,
                          OAuth2CodeStore oAuth2CodeStore) {
        this.authService = authService;
        this.magicLinkService = magicLinkService;
        this.oAuth2CodeStore = oAuth2CodeStore;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        setAuthCookies(response, authResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        setAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        String token = (request != null && request.refreshToken() != null && !request.refreshToken().isBlank())
                ? request.refreshToken()
                : refreshTokenCookie;
        if (token == null || token.isBlank()) {
            throw new com.surprise.mortgagesurprise.exception.BusinessException("Refresh token is required");
        }
        AuthResponse authResponse = authService.refreshToken(new RefreshTokenRequest(token));
        setAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        return ResponseEntity.ok(new AuthResponse(
                null,
                null,
                "Bearer",
                0L,
                userDetails.getId(),
                userDetails.getEmail(),
                role
        ));
    }

    @PostMapping("/magic-link")
    public ResponseEntity<MagicLinkResponse> requestMagicLink(@Valid @RequestBody MagicLinkRequest request) {
        magicLinkService.requestLink(request.email());
        return ResponseEntity.accepted()
                .body(new MagicLinkResponse("If an account exists for this email, a sign-in link has been sent."));
    }

    @PostMapping("/magic-link/verify")
    public ResponseEntity<AuthResponse> verifyMagicLink(
            @Valid @RequestBody MagicLinkVerifyRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.createAuthResponse(magicLinkService.consume(request.token()));
        setAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/oauth2/exchange")
    public ResponseEntity<AuthResponse> exchangeOAuth2Code(
            @Valid @RequestBody OAuth2CodeRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = oAuth2CodeStore.consume(request.code())
                .orElseThrow(() -> new com.surprise.mortgagesurprise.exception.BusinessException(
                        "Invalid or expired OAuth2 code"));
        setAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        String token = (request != null && request.refreshToken() != null && !request.refreshToken().isBlank())
                ? request.refreshToken()
                : refreshTokenCookie;
        if (userDetails != null && token != null) {
            authService.logout(userDetails.getId(), token);
        }
        clearAuthCookies(response);
        return ResponseEntity.noContent().build();
    }

    private void setAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
        if (authResponse.accessToken() != null) {
            ResponseCookie accessCookie = ResponseCookie.from("accessToken", authResponse.accessToken())
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ofMinutes(15))
                    .sameSite("Lax")
                    .secure(false)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        }

        if (authResponse.refreshToken() != null) {
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", authResponse.refreshToken())
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ofDays(7))
                    .sameSite("Lax")
                    .secure(false)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }
    }

    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .secure(false)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .secure(false)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}
