package com.surprise.mortgagesurprise.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public final class TwelveDataResponseParser {

    private TwelveDataResponseParser() {
    }

    public static Map<String, BigDecimal> parseRates(JsonNode root) {
        if (root == null) {
            return Collections.emptyMap();
        }

        if (root.has("code") && root.has("message")) {
            log.warn("TwelveData returned API error: [{}] {}",
                    root.get("code").asInt(), root.get("message").asText());
            return Collections.emptyMap();
        }

        Map<String, BigDecimal> rates = new HashMap<>();

        if (root.has("price")) {
            rates.put("DEFAULT", new BigDecimal(root.get("price").asText()));
            return rates;
        }

        root.propertyNames().forEach(pair -> {
            JsonNode pairNode = root.get(pair);
            if (pairNode != null && pairNode.has("price")) {
                String targetCurrency = pair.split("/")[0].trim().toUpperCase();
                rates.put(targetCurrency, new BigDecimal(pairNode.get("price").asText()));
            } else if (pairNode != null && pairNode.has("status") && "error".equalsIgnoreCase(pairNode.get("status").asText())) {
                log.warn("TwelveData symbol [{}] error: {}", pair,
                        pairNode.has("message") ? pairNode.get("message").asText() : "Unknown");
            }
        });

        return rates;
    }
}
