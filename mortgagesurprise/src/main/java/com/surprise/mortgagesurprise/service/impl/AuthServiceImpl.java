package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.auth.AuthResponse;
import com.surprise.mortgagesurprise.dto.auth.LoginRequest;
import com.surprise.mortgagesurprise.dto.auth.RefreshTokenRequest;
import com.surprise.mortgagesurprise.dto.auth.RegisterRequest;
import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.exception.ResourceNotFoundException;
import com.surprise.mortgagesurprise.messaging.event.EmailNotificationEvent;
import com.surprise.mortgagesurprise.messaging.producer.MessageProducer;
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
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final MessageProducer messageProducer;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           AuthenticationManager authenticationManager,
                           RefreshTokenService refreshTokenService,
                           MessageProducer messageProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.messageProducer = messageProducer;
    }

    @Override
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException("User with email " + req.email() + " already exists");
        }

        User user = new User(
                req.email(),
                passwordEncoder.encode(req.password()),
                req.firstName(),
                req.lastName(),
                Role.ROLE_USER
        );

        User savedUser = userRepository.save(user);

        String confirmationToken = UUID.randomUUID().toString();
        EmailNotificationEvent emailEvent = new EmailNotificationEvent(
                savedUser.getEmail(),
                "Confirm your registration on MortgageSurprise",
                "Welcome! Please confirm your registration using token: " + confirmationToken,
                "EMAIL_CONFIRMATION",
                Instant.now()
        );
        messageProducer.sendEmailNotification(emailEvent);

        String accessToken = tokenProvider.generateAccessToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getId(), savedUser.getEmail());

        refreshTokenService.storeRefreshToken(savedUser.getId(), refreshToken, Duration.ofMillis(tokenProvider.getRefreshTokenValidityMs()));

        return AuthResponse.of(accessToken, refreshToken, 900000, savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userDetails.getUsername()));

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        refreshTokenService.storeRefreshToken(user.getId(), refreshToken, Duration.ofMillis(tokenProvider.getRefreshTokenValidityMs()));

        return AuthResponse.of(accessToken, refreshToken, 900000, user.getId(), user.getEmail(), user.getRole().name());
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest req) {
        String oldRefreshToken = req.refreshToken();
        if (!tokenProvider.validateToken(oldRefreshToken)) {
            throw new BusinessException("Invalid or expired refresh token");
        }

        UUID userId = tokenProvider.getUserIdFromToken(oldRefreshToken);
        if (!refreshTokenService.isRefreshTokenValid(userId, oldRefreshToken)) {
            throw new BusinessException("Refresh token is revoked or no longer active");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        refreshTokenService.revokeRefreshToken(userId, oldRefreshToken);

        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        refreshTokenService.storeRefreshToken(user.getId(), newRefreshToken, Duration.ofMillis(tokenProvider.getRefreshTokenValidityMs()));

        return AuthResponse.of(newAccessToken, newRefreshToken, 900000, user.getId(), user.getEmail(), user.getRole().name());
    }

    @Override
    public void logout(UUID userId, String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revokeRefreshToken(userId, refreshToken);
        } else {
            throw new BusinessException("Invalid or expired refresh token");
        }
    }
}
