package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.auth.AuthResponse;
import com.surprise.mortgagesurprise.dto.auth.LoginRequest;
import com.surprise.mortgagesurprise.dto.auth.RefreshTokenRequest;
import com.surprise.mortgagesurprise.dto.auth.RegisterRequest;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.model.entity.Role;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.repository.jpa.UserRepository;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.security.jwt.JwtTokenProvider;
import com.surprise.mortgagesurprise.service.AuthService;
import com.surprise.mortgagesurprise.service.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           AuthenticationManager authenticationManager,
                           RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public AuthResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("An account with this email already exists");
        }

        User user = new User(
                email,
                passwordEncoder.encode(req.password()),
                req.firstName(),
                req.lastName(),
                Role.ROLE_USER
        );

        User savedUser = userRepository.save(user);
        return createAuthResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(req.email()), req.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userDetails.getUsername()));

        return createAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest req) {
        String oldRefreshToken = req.refreshToken();
        if (!tokenProvider.validateRefreshToken(oldRefreshToken)) {
            throw new BusinessException("Invalid or expired refresh token");
        }

        UUID userId = tokenProvider.getUserIdFromToken(oldRefreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (!refreshTokenService.consumeRefreshToken(userId, oldRefreshToken)) {
            throw new BusinessException("Refresh token is revoked or no longer active");
        }
        return createAuthResponse(user);
    }

    @Override
    public void logout(UUID userId, String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)
                || !userId.equals(tokenProvider.getUserIdFromToken(refreshToken))
                || !refreshTokenService.consumeRefreshToken(userId, refreshToken)) {
            throw new BusinessException("Invalid or expired refresh token");
        }
    }

    @Override
    public AuthResponse createAuthResponse(User user) {
        if (!user.isEnabled()) {
            throw new BusinessException("This account is disabled");
        }

        String accessToken = tokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        refreshTokenService.storeRefreshToken(
                user.getId(), refreshToken, Duration.ofMillis(tokenProvider.getRefreshTokenValidityMs()));
        return AuthResponse.of(accessToken, refreshToken, tokenProvider.getAccessTokenValidityMs(),
                user.getId(), user.getEmail(), user.getRole().name());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
