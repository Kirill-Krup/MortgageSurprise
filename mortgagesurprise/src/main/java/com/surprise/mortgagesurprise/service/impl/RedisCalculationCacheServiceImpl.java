package com.surprise.mortgagesurprise.service.impl;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import com.surprise.mortgagesurprise.dto.calculator.CalculationResponse;
import com.surprise.mortgagesurprise.service.CalculationCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisCalculationCacheServiceImpl implements CalculationCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCalculationCacheServiceImpl.class);
    private static final String KEY_PREFIX = "cache:calc:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<CalculationResponse> getFromCache(String calculationHash) {
        String key = KEY_PREFIX + calculationHash;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                CalculationResponse response = objectMapper.readValue(json, CalculationResponse.class);
                log.info("Redis Cache HIT: calculation found for hash {}", calculationHash);
                return Optional.of(response);
            }
        } catch (Exception e) {
            log.error("Error reading from Redis cache for key {}: {}", key, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void putInCache(String calculationHash, CalculationResponse response) {
        putInCache(calculationHash, response, DEFAULT_TTL);
    }

    @Override
    public void putInCache(String calculationHash, CalculationResponse response, Duration ttl) {
        String key = KEY_PREFIX + calculationHash;
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.info("Redis Cache PUT: calculation cached (hash={}, ttl={})", calculationHash, ttl);
        } catch (Exception e) {
            log.error("Error saving to Redis cache for key {}: {}", key, e.getMessage());
        }
    }
}