package com.surprise.mortgagesurprise.util;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TwelveDataResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseRates_shouldExtractValidRatesAndIgnoreErrors() throws Exception {
        String json = """
                {
                  "USD/RUB": { "price": "82.0050" },
                  "EUR/RUB": { "price": "90.1500" },
                  "BYN/RUB": { "code": 404, "message": "symbol not found", "status": "error" }
                }
                """;
        JsonNode root = objectMapper.readTree(json);

        Map<String, BigDecimal> rates = TwelveDataResponseParser.parseRates(root);

        assertEquals(2, rates.size());
        assertEquals(new BigDecimal("82.0050"), rates.get("USD"));
        assertEquals(new BigDecimal("90.1500"), rates.get("EUR"));
        assertNull(rates.get("BYN"));
    }

    @Test
    void parseRates_shouldReturnEmptyMapOnApiError() throws Exception {
        String json = """
                {
                  "code": 401,
                  "message": "Invalid API key",
                  "status": "error"
                }
                """;
        JsonNode root = objectMapper.readTree(json);

        Map<String, BigDecimal> rates = TwelveDataResponseParser.parseRates(root);

        assertTrue(rates.isEmpty());
    }

    @Test
    void parseRates_shouldReturnEmptyMapWhenRootIsNull() {
        Map<String, BigDecimal> rates = TwelveDataResponseParser.parseRates(null);

        assertTrue(rates.isEmpty());
    }
}
