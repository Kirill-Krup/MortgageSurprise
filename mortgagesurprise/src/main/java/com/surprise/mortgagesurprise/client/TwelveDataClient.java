package com.surprise.mortgagesurprise.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class TwelveDataClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String symbols;

    public TwelveDataClient(
            @Value("${twelvedata.base-url:https://api.twelvedata.com}") String baseUrl,
            @Value("${twelvedata.api-key}") String apiKey,
            @Value("${twelvedata.symbols}") String symbols) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.symbols = symbols;
    }

    public Map<String, BigDecimal> fetchLatestRates() {
        Map<String, BigDecimal> rates = new HashMap<>();
        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/price")
                            .queryParam("symbol", symbols)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                return rates;
            }

            if (root.has("code") && root.has("message")) {
                log.warn("TwelveData returned API error: [{}] {}",
                        root.get("code").asInt(), root.get("message").asText());
                return rates;
            }

            root.propertyNames().forEach(pair -> {
                JsonNode pairNode = root.get(pair);
                if (pairNode != null && pairNode.has("price")) {
                    String targetCurrency = pair.split("/")[0].trim().toUpperCase();
                    rates.put(targetCurrency, new BigDecimal(pairNode.get("price").asText()));
                }
            });
        } catch (Exception e) {
            log.error("Failed to fetch rates from TwelveData: {}", e.getMessage());
        }
        return rates;
    }
}