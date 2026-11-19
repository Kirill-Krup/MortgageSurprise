package com.surprise.mortgagesurprise.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("TwelveDataResponseParser Tests")
class TwelveDataResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should parse multi-pair prices correctly")
    void shouldParseMultiPairPrices() throws Exception {
        String json = """
                {
                    "USD/RUB": { "price": "82.50000" },
                    "EUR/RUB": { "price": "90.20000" },
                    "BYN/RUB": { "price": "28.40000" }
                }
                """;
        JsonNode root = objectMapper.readTree(json);

        Map<String, BigDecimal> rates = TwelveDataResponseParser.parseRates(root);

        assertThat(rates).hasSize(3);
        assertThat(rates.get("USD")).isEqualByComparingTo("82.50000");
        assertThat(rates.get("EUR")).isEqualByComparingTo("90.20000");
        assertThat(rates.get("BYN")).isEqualByComparingTo("28.40000");
    }

    @Test
    @DisplayName("Should parse single pair response with symbol")
    void shouldParseSinglePairResponse() throws Exception {
        String json = """
                {
                    "symbol": "USD/RUB",
                    "price": "82.75"
                }
                """;
        JsonNode root = objectMapper.readTree(json);

        Map<String, BigDecimal> rates = TwelveDataResponseParser.parseRates(root);

        assertThat(rates).hasSize(1);
        assertThat(rates.get("USD")).isEqualByComparingTo("82.75");
    }

    @Test
    @DisplayName("Should return empty map on null or API error payload")
    void shouldReturnEmptyMapOnError() throws Exception {
        String errorJson = """
                {
                    "code": 401,
                    "message": "Invalid API key"
                }
                """;
        JsonNode root = objectMapper.readTree(errorJson);

        Map<String, BigDecimal> rates = TwelveDataResponseParser.parseRates(root);
        assertThat(rates).isEmpty();

        assertThat(TwelveDataResponseParser.parseRates(null)).isEmpty();
    }
}
