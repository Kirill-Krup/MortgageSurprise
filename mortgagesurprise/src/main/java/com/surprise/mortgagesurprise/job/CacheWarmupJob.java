package com.surprise.mortgagesurprise.job;

import com.surprise.mortgagesurprise.config.CacheConfig;
import com.surprise.mortgagesurprise.model.entity.MicroloanConfig;
import com.surprise.mortgagesurprise.model.entity.MicroloanRate;
import com.surprise.mortgagesurprise.repository.jpa.MicroloanConfigRepository;
import com.surprise.mortgagesurprise.repository.jpa.MicroloanRateRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupJob {

    private final MicroloanRateRepository rateRepository;
    private final MicroloanConfigRepository configRepository;
    private final CaffeineCacheManager caffeineCacheManager;
    private final EntityManager entityManager;

    private static final String DEFAULT_CONFIG_ID = "DEFAULT";

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional(readOnly = true)
    public void warmup() {
        log.info("Cache warmup started");
        try {
            warmupRates();
            warmupConfig();
            log.info("Cache warmup finished successfully");
        } catch (Exception e) {
            log.error("Cache warmup failed", e);
        }
    }

    private void warmupRates() {
        var cache = caffeineCacheManager.getCache(CacheConfig.MICROLOAN_RATES);
        if (cache == null) {
            log.warn("Cache '{}' not found, skip warmup", CacheConfig.MICROLOAN_RATES);
            return;
        }
        var rates = rateRepository.findAll();
        for (MicroloanRate rate : rates) {
            entityManager.detach(rate);
            cache.put(rate.getName(), rate);
        }
        log.info("Warmed up {} microloan rates", rates.size());
    }

    private void warmupConfig() {
        var cache = caffeineCacheManager.getCache(CacheConfig.MICROLOAN_CONFIGS);
        if (cache == null) {
            log.warn("Cache '{}' not found, skip warmup", CacheConfig.MICROLOAN_CONFIGS);
            return;
        }
        var config = configRepository.findById(DEFAULT_CONFIG_ID)
                .orElseGet(() -> new MicroloanConfig(DEFAULT_CONFIG_ID, new BigDecimal("2000")));
        entityManager.detach(config);
        cache.put("DEFAULT", config);
        log.info("Warmed up microloan config");
    }
}