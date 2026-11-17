package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.adapter.AuthServiceAdapter;
import com.surprise.mortgagesurprise.dto.auth.*;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.security.oauth2.OAuth2CodeStore;
import com.surprise.mortgagesurprise.service.AuthService;
import com.surprise.mortgagesurprise.service.impl.MagicLinkService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.surprise.mortgagesurprise.util.CookieManager.clearAuthCookies;
import static com.surprise.mortgagesurprise.util.CookieManager.setAuthCookies;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthServiceAdapter authServiceAdapter;
    private final MagicLinkService magicLinkService;
    private final OAuth2CodeStore oAuth2CodeStore;

    public AuthController(AuthService authService, AuthServiceAdapter authServiceAdapter,
                          MagicLinkService magicLinkService,
                          OAuth2CodeStore oAuth2CodeStore) {
        this.authService = authService;
        this.authServiceAdapter = authServiceAdapter;
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
        var authResponse = authServiceAdapter.refresh(request, refreshTokenCookie);
        setAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authServiceAdapter.getCurrentUser(userDetails));
    }

    @PostMapping("/magic-link")
    public ResponseEntity<MagicLinkResponse> requestMagicLink(@Valid @RequestBody MagicLinkRequest request) {
        return ResponseEntity.accepted()
                .body(magicLinkService.requestLink(request.email()));
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
                .orElseThrow(() -> new BusinessException(
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
        authServiceAdapter.logout(userDetails, request, refreshTokenCookie);
        clearAuthCookies(response);
        return ResponseEntity.noContent().build();
    }
}