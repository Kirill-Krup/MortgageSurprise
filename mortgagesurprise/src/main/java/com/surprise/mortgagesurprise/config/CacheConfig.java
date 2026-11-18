package com.surprise.mortgagesurprise.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String MICROLOAN_CONFIGS = "microloan_configs";
    public static final String MICROLOAN_RATES = "microloan_rates";

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        var manager = new CaffeineCacheManager(MICROLOAN_CONFIGS,
                MICROLOAN_RATES);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats());
        return manager;
    }
}