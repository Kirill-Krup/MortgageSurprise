package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.surprise.mortgagesurprise.util.Sha256Encoder.toSha256;

@Service
public class RedisRefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RedisRefreshTokenServiceImpl.class);
    private static final String KEY_PREFIX = "auth:refresh:";
    private static final DefaultRedisScript<Long> CONSUME_IF_OWNER = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long.class);

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
    public boolean consumeRefreshToken(UUID userId, String token) {
        String key = buildKey(token);
        Long result = redisTemplate.execute(CONSUME_IF_OWNER, List.of(key), userId.toString());
        return Long.valueOf(1).equals(result);
    }

    private String buildKey(String token) {
        return KEY_PREFIX + toSha256(token);
    }
}