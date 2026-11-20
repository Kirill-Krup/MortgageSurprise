package com.surprise.mortgagesurprise.service.impl;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import com.surprise.mortgagesurprise.dto.calculator.microloan.MicroloanCalculationResponse;
import com.surprise.mortgagesurprise.service.MicroloanCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisMicroloanCacheServiceImpl implements MicroloanCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisMicroloanCacheServiceImpl.class);
    private static final String KEY_PREFIX = "cache:microloan:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<MicroloanCalculationResponse> getFromCache(String calculationHash) {
        String key = KEY_PREFIX + calculationHash;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                MicroloanCalculationResponse response = objectMapper.readValue(json, MicroloanCalculationResponse.class);
                log.info("Redis Microloan Cache HIT for hash {}", calculationHash);
                return Optional.of(response);
            }
        } catch (Exception e) {
            log.error("Redis microloan cache read error for key {}: {}", key, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void putInCache(String calculationHash, MicroloanCalculationResponse response) {
        putInCache(calculationHash, response, DEFAULT_TTL);
    }

    @Override
    public void putInCache(String calculationHash, MicroloanCalculationResponse response, Duration ttl) {
        String key = KEY_PREFIX + calculationHash;
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.info("Redis Microloan Cache PUT for hash {} (ttl={})", calculationHash, ttl);
        } catch (Exception e) {
            log.error("Redis microloan cache put error for key {}: {}", key, e.getMessage());
        }
    }
}
