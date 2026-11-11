package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.messaging.event.EmailNotificationEvent;
import com.surprise.mortgagesurprise.messaging.producer.MessageProducer;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.repository.jpa.UserRepository;
import com.surprise.mortgagesurprise.util.Sha256Encoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class MagicLinkService {

    private static final String KEY_PREFIX = "auth:magic-link:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final MessageProducer messageProducer;
    private final String frontendUrl;

    public MagicLinkService(UserRepository userRepository,
                            StringRedisTemplate redisTemplate,
                            MessageProducer messageProducer,
                            @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.messageProducer = messageProducer;
        this.frontendUrl = frontendUrl;
    }

    public void requestLink(String emailAddress) {
        String email = emailAddress.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmail(email)
                .filter(User::isEnabled)
                .ifPresent(user -> sendLink(user.getEmail()));
    }

    @Transactional
    public User consume(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("Invalid or expired magic link");
        }

        String email = redisTemplate.opsForValue().getAndDelete(key(token));
        if (email == null) {
            throw new BusinessException("Invalid or expired magic link");
        }

        User user = userRepository.findByEmail(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> new BusinessException("Invalid or expired magic link"));
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            return userRepository.save(user);
        }
        return user;
    }

    private void sendLink(String email) {
        byte[] randomToken = new byte[32];
        SECURE_RANDOM.nextBytes(randomToken);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        redisTemplate.opsForValue().set(key(token), email, TOKEN_TTL);

        String link = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/magic-link")
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
        messageProducer.sendEmailNotification(new EmailNotificationEvent(
                email,
                "Your MortgageSurprise sign-in link",
                "Use this link within 15 minutes to sign in: " + link,
                "MAGIC_LINK",
                Instant.now()));
    }

    private String key(String token) {
        return KEY_PREFIX + Sha256Encoder.toSha256(token);
    }
}
