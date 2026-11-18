package com.surprise.mortgagesurprise.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateRedisCache {

    private static final String REDIS_KEY = "exchange_rates:latest";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<Map<String, BigDecimal>> getFromCache() {
        try {
            String cached = redisTemplate.opsForValue().get(REDIS_KEY);
            if (cached != null) {
                return Optional.of(objectMapper.readValue(cached, new TypeReference<Map<String, BigDecimal>>() {}));
            }
        } catch (Exception e) {
            log.warn("Redis read failed for exchange rates: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public void putToCache(Map<String, BigDecimal> rates) {
        try {
            Map<String, BigDecimal> payload = new HashMap<>(rates);
            payload.put("RUB", BigDecimal.ONE);
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(REDIS_KEY, json, Duration.ofHours(24));
        } catch (Exception e) {
            log.warn("Failed to cache rates in Redis: {}", e.getMessage());
        }
    }
}
