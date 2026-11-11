package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static com.surprise.mortgagesurprise.util.Sha256Encoder.toSha256;

@Service
public class RedisRefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RedisRefreshTokenServiceImpl.class);
    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void storeRefreshToken(UUID userId, String token, Duration duration) {
        String key = buildKey(token);
        try {
            redisTemplate.opsForValue().set(key, userId.toString(), duration);
        } catch (Exception e) {
            log.error("Failed to store refresh token for user {}: {}", userId, e.getMessage());
            throw new IllegalStateException("Failed to store refresh token", e);
        }
    }

    @Override
    public boolean isRefreshTokenValid(UUID userId, String token) {
        String key = buildKey(token);
        try {
            var storedUserId = redisTemplate.opsForValue().get(key);
            return userId.toString().equals(storedUserId);
        } catch (Exception e) {
            log.error("Failed to validate refresh token in Redis: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void revokeRefreshToken(UUID userId, String token) {
        String key = buildKey(token);
        log.info("Revoking refresh token in Redis for user: {}", userId);
        try {
            var value = Optional.ofNullable(redisTemplate.opsForValue().get(key));
            if (value.isPresent() && isOwner(userId, value.get())) {
                redisTemplate.delete(key);
                log.info("Refresh token revoked for user: {}", userId);
            } else {
                log.warn("Attempt to revoke non-existent or foreign refresh token, user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to revoke refresh token in Redis: {}", e.getMessage());
        }
    }

    private String buildKey(String token) {
        return KEY_PREFIX + toSha256(token);
    }

    private boolean isOwner(UUID userId, String storedUserId) {
        return userId.toString().equals(storedUserId);
    }
}