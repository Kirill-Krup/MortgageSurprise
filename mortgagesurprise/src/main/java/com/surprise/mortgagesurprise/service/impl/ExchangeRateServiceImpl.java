package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.model.entity.ExchangeRateEntity;
import com.surprise.mortgagesurprise.repository.jpa.ExchangeRateRepository;
import com.surprise.mortgagesurprise.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository repository;
    private final ExchangeRateRedisCache redisCache;

    @Override
    public Map<String, BigDecimal> getRates() {
        Optional<Map<String, BigDecimal>> cached = redisCache.getFromCache();
        if (cached.isPresent()) {
            return cached.get();
        }

        Map<String, BigDecimal> map = new HashMap<>();
        map.put("RUB", BigDecimal.ONE);

        List<ExchangeRateEntity> latest = repository.findLatestRatesDistinct();
        for (ExchangeRateEntity entity : latest) {
            map.put(entity.getTargetCurrency(), entity.getRate());
        }

        // Fallback default seeds if DB has no rates for target currencies yet
        map.putIfAbsent("USD", new BigDecimal("92.5000"));
        map.putIfAbsent("EUR", new BigDecimal("100.2000"));
        map.putIfAbsent("BYN", new BigDecimal("28.4000"));

        redisCache.putToCache(map);
        return map;
    }
}
