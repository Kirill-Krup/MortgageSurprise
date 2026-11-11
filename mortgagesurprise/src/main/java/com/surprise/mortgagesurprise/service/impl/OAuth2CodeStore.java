package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.auth.AuthResponse;
import com.surprise.mortgagesurprise.util.Sha256Encoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Service
public class OAuth2CodeStore {

    private static final String KEY_PREFIX = "auth:oauth2:code:";
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public OAuth2CodeStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public String save(AuthResponse authResponse) {
        byte[] randomCode = new byte[32];
        SECURE_RANDOM.nextBytes(randomCode);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(randomCode);
        String value = objectMapper.writeValueAsString(authResponse);
        redis.opsForValue().set(key(code), value, TTL);
        return code;
    }

    public Optional<AuthResponse> consume(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String value = redis.opsForValue().getAndDelete(key(code));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(value, AuthResponse.class));
    }

    private String key(String code) {
        return KEY_PREFIX + Sha256Encoder.toSha256(code);
    }
}
