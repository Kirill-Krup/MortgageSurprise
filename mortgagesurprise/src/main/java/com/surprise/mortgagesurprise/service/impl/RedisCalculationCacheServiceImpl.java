package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.service.CalculationCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisCalculationCacheServiceImpl implements CalculationCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCalculationCacheServiceImpl.class);
    private static final String KEY_PREFIX = "cache:calc:";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCalculationCacheServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<CalculationResponse> getFromCache(String calculationHash) {
        String key = KEY_PREFIX + calculationHash;
        try {
            // TODO: [Team Task] Cast/deserialize calculation result from Redis cache
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj instanceof CalculationResponse response) {
                log.info("Calculation cache hit for hash: {}", calculationHash);
                return Optional.of(response);
            }
        } catch (Exception e) {
            log.error("Failed to read calculation from Redis cache: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void putInCache(String calculationHash, CalculationResponse response, Duration ttl) {
        String key = KEY_PREFIX + calculationHash;
        try {
            // TODO: [Team Task] Store calculation response in Redis with TTL
            redisTemplate.opsForValue().set(key, response, ttl);
            log.info("Cached calculation response for hash: {}", calculationHash);
        } catch (Exception e) {
            log.error("Failed to store calculation in Redis cache: {}", e.getMessage());
        }
    }
}
