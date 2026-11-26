package com.idfinance.credit_scoring_service.service.impl;

import com.idfinance.credit_scoring_service.domain.rule_set.Identifiable;
import com.idfinance.credit_scoring_service.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisCacheService implements CacheService {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String IDS_SUFFIX = ":__ids";

    private final RedisTemplate<String, Object> redisObjectTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public <T extends Identifiable<?>> void put(String key, T value) {
        redisObjectTemplate.opsForValue().set(key, value, TTL);
        String id = extractId(value);
        if (id != null) {
            stringRedisTemplate.opsForSet().add(namespaceOf(key) + IDS_SUFFIX, id);
        }
    }

    @Override
    public <T extends Identifiable<?>> void putAll(String namespace, Set<T> values) {
        values.forEach(v -> {
            String id = extractId(v);
            if (id != null) {
                put(namespace + ":" + id, v);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisObjectTemplate.opsForValue().get(key);
    }

    @Override
    public void remove(String key) {
        redisObjectTemplate.delete(key);
        int idx = key.lastIndexOf(':');
        if (idx > 0) {
            String namespace = key.substring(0, idx);
            String id = key.substring(idx + 1);
            stringRedisTemplate.opsForSet().remove(namespace + IDS_SUFFIX, id);
        }
    }

    private <T extends Identifiable<?>> String extractId(T value) {
        return value.getId().toString();
    }

    private String namespaceOf(String key) {
        int idx = key.lastIndexOf(':');
        return idx > 0 ? key.substring(0, idx) : key;
    }
}