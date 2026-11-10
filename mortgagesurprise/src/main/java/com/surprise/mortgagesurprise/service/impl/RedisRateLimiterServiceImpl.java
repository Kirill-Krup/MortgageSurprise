package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.service.RateLimiterRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisRateLimiterServiceImpl implements RateLimiterRedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterServiceImpl.class);
    private static final String KEY_PREFIX = "ratelimit:";
    private static final int DEFAULT_LIMIT_PER_MINUTE = 30;

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiterServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean allowRequest(String clientIdentifier, String action) {
        String key = KEY_PREFIX + action + ":" + clientIdentifier;
        try {
            // TODO: [Team Task] Implement sliding window counter or token bucket in Redis.
            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            boolean allowed = currentCount == null || currentCount <= DEFAULT_LIMIT_PER_MINUTE;
            if (!allowed) {
                log.warn("Rate limit exceeded for {} on action {}", clientIdentifier, action);
            }
            return allowed;
        } catch (Exception e) {
            log.error("Redis rate limiter error, allowing request by default: {}", e.getMessage());
            return true;
        }
    }
}
