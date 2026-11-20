package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.client.TwelveDataClient;
import com.surprise.mortgagesurprise.model.entity.ExchangeRateEntity;
import com.surprise.mortgagesurprise.repository.jpa.ExchangeRateRepository;
import com.surprise.mortgagesurprise.util.TwelveDataResponseParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@Slf4j
public class ExchangeRateRefresher {

    private final ExchangeRateRepository repository;
    private final TwelveDataClient twelveDataClient;
    private final ExchangeRateRedisCache redisCache;
    private final String apiKey;
    private final String symbols;

    public ExchangeRateRefresher(
            ExchangeRateRepository repository,
            TwelveDataClient twelveDataClient,
            ExchangeRateRedisCache redisCache,
            @Value("${twelvedata.api-key}") String apiKey,
            @Value("${twelvedata.symbols}") String symbols) {
        this.repository = repository;
        this.twelveDataClient = twelveDataClient;
        this.redisCache = redisCache;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.symbols = symbols;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        Instant dayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        if (!repository.existsByUpdatedAtAfter(dayAgo)) {
            log.info("No fresh exchange rates found in DB (older than 24h). Triggering TwelveData fetch...");
            refreshRates();
        } else {
            log.info("Exchange rates in DB are fresh (< 24h). Skipping external call.");
            warmUpCache();
        }
    }

    @Scheduled(cron = "${twelvedata.cron:0 0 9 * * *}")
    public void scheduledDailyRefresh() {
        log.info("Triggered scheduled daily exchange rates update.");
        refreshRates();
    }

    @Transactional
    public void refreshRates() {
        Map<String, BigDecimal> externalRates = fetchLatestRates();
        Instant now = Instant.now();

        if (externalRates.isEmpty()) {
            log.warn("TwelveData returned no rates (offline or quota exceeded). Keeping current DB records.");
            warmUpCache();
            return;
        }

        List<ExchangeRateEntity> toSave = new ArrayList<>();
        externalRates.forEach((target, rate) -> {
            toSave.add(ExchangeRateEntity.builder()
                    .baseCurrency("RUB")
                    .targetCurrency(target)
                    .rate(rate)
                    .source("TWELVE_DATA")
                    .updatedAt(now)
                    .build());
        });

        repository.saveAll(toSave);
        redisCache.putToCache(externalRates);
        log.info("Saved and cached {} fresh exchange rates.", toSave.size());
    }

    private Map<String, BigDecimal> fetchLatestRates() {
        try {
            JsonNode root = twelveDataClient.getPrice(symbols, apiKey);
            return TwelveDataResponseParser.parseRates(root);
        } catch (Exception e) {
            log.error("Failed to fetch rates from TwelveData: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private void warmUpCache() {
        Map<String, BigDecimal> map = new HashMap<>();
        map.put("RUB", BigDecimal.ONE);
        repository.findLatestRatesDistinct().forEach(e ->
                map.put(e.getTargetCurrency(), e.getRate()));
        redisCache.putToCache(map);
    }
}
