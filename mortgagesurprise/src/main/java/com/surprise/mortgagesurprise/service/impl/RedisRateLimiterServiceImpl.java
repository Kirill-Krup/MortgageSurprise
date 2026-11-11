package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.service.RateLimiterRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RedisRateLimiterServiceImpl implements RateLimiterRedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterServiceImpl.class);
    private static final String KEY_PREFIX = "rate-limit:";
    private static final int DEFAULT_LIMIT_PER_MINUTE = 30;
    private static final int TTL_DURATION_IN_MINUTES = 1;

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiterServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean allowRequest(UUID userId, String action) {
        String key = KEY_PREFIX + action + ":" + userId.toString();
        try {
            var currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount != null && currentCount == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(TTL_DURATION_IN_MINUTES));
            }
            var isAllowed = isInLimit(currentCount);
            if (isAllowed) {
                log.info("Allowing request for user: {}, action: {}", userId, action);
            } else {
                log.warn("Rate limit exceeded for user: {}, action: {}", userId, action);
            }
            return isAllowed;
        } catch (Exception e) {
            log.error("Redis rate limiter error, allowing request by default: {}", e.getMessage());
            return true;
        }
    }

    private boolean isInLimit(Long currentCount) {
        return currentCount != null && currentCount <= DEFAULT_LIMIT_PER_MINUTE;
    }
}