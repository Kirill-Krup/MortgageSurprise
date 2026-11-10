package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

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
        String key = buildKey(userId, token);
        log.info("Storing refresh token in Redis for user {}: key={}, ttl={}", userId, key, duration);
        try {
            // TODO: [Team Task] Optionally customize token storage structure or use Redis Hash
            redisTemplate.opsForValue().set(key, "ACTIVE", duration);
        } catch (Exception e) {
            log.error("Failed to store refresh token in Redis: {}", e.getMessage());
        }
    }

    @Override
    public boolean isRefreshTokenValid(UUID userId, String token) {
        String key = buildKey(userId, token);
        try {
            // TODO: [Team Task] Verify against blacklist or active token keys
            String value = redisTemplate.opsForValue().get(key);
            return "ACTIVE".equalsIgnoreCase(value);
        } catch (Exception e) {
            log.error("Failed to validate refresh token in Redis: {}", e.getMessage());
            return true;
        }
    }

    @Override
    public void revokeRefreshToken(UUID userId, String token) {
        String key = buildKey(userId, token);
        log.info("Revoking refresh token in Redis for user {}: key={}", userId, key);
        try {
            // TODO: [Team Task] Delete token key or mark as REVOKED
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to revoke refresh token in Redis: {}", e.getMessage());
        }
    }

    @Override
    public void revokeAllUserTokens(UUID userId) {
        log.info("Revoking all refresh tokens for user {}", userId);
        try {
            // TODO: [Team Task] Implement pattern scan "auth:refresh:{userId}:*" or store user tokens set
            var keys = redisTemplate.keys(KEY_PREFIX + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.error("Failed to revoke all user tokens in Redis: {}", e.getMessage());
        }
    }

    private String buildKey(UUID userId, String token) {
        int tokenHash = token.hashCode();
        return KEY_PREFIX + userId + ":" + tokenHash;
    }
}
