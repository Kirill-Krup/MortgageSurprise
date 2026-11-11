package com.surprise.mortgagesurprise.controller;

import com.surprise.mortgagesurprise.dto.auth.AuthResponse;
import com.surprise.mortgagesurprise.dto.auth.LoginRequest;
import com.surprise.mortgagesurprise.dto.auth.MagicLinkRequest;
import com.surprise.mortgagesurprise.dto.auth.MagicLinkResponse;
import com.surprise.mortgagesurprise.dto.auth.MagicLinkVerifyRequest;
import com.surprise.mortgagesurprise.dto.auth.OAuth2CodeRequest;
import com.surprise.mortgagesurprise.dto.auth.RefreshTokenRequest;
import com.surprise.mortgagesurprise.dto.auth.RegisterRequest;
import com.surprise.mortgagesurprise.service.AuthService;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.impl.MagicLinkService;
import com.surprise.mortgagesurprise.service.impl.OAuth2CodeStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/magic-link")
    public ResponseEntity<MagicLinkResponse> requestMagicLink(@Valid @RequestBody MagicLinkRequest request) {
        magicLinkService.requestLink(request.email());
        return ResponseEntity.accepted()
                .body(new MagicLinkResponse("If an account exists for this email, a sign-in link has been sent."));
    }

    @PostMapping("/magic-link/verify")
    public ResponseEntity<AuthResponse> verifyMagicLink(
            @Valid @RequestBody MagicLinkVerifyRequest request) {
        return ResponseEntity.ok(authService.createAuthResponse(magicLinkService.consume(request.token())));
    }

    @PostMapping("/oauth2/exchange")
    public ResponseEntity<AuthResponse> exchangeOAuth2Code(@Valid @RequestBody OAuth2CodeRequest request) {
        AuthResponse response = oAuth2CodeStore.consume(request.code())
                .orElseThrow(() -> new com.surprise.mortgagesurprise.exception.BusinessException(
                        "Invalid or expired OAuth2 code"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        authService.logout(userDetails.getId(), request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
