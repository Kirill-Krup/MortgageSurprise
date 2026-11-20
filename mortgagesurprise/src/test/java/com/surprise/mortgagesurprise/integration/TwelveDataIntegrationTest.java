package com.surprise.mortgagesurprise.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.surprise.mortgagesurprise.client.TwelveDataClient;
import com.surprise.mortgagesurprise.service.ExchangeRateService;
import com.surprise.mortgagesurprise.service.impl.ExchangeRateRefresher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@DisplayName("TwelveData Integration WireMock Test")
class TwelveDataIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TwelveDataClient twelveDataClient;

    @Autowired
    private ExchangeRateRefresher refresher;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("TwelveDataClient should fetch prices via WireMock without real network calls")
    void shouldFetchPricesFromWireMock() {
        // Arrange
        wireMockServer.stubFor(get(urlPathEqualTo("/price"))
                .withQueryParam("symbol", equalTo("USD/RUB,EUR/RUB,BYN/RUB"))
                .withQueryParam("apikey", equalTo("test-api-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "USD/RUB": { "price": "83.15000" },
                                    "EUR/RUB": { "price": "91.45000" },
                                    "BYN/RUB": { "price": "29.10000" }
                                }
                                """)));

        // Act
        JsonNode response = twelveDataClient.getPrice("USD/RUB,EUR/RUB,BYN/RUB", "test-api-key");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.has("USD/RUB")).isTrue();
        assertThat(response.get("USD/RUB").get("price").asText()).isEqualTo("83.15000");

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/price"))
                .withQueryParam("symbol", equalTo("USD/RUB,EUR/RUB,BYN/RUB"))
                .withQueryParam("apikey", equalTo("test-api-key")));
    }

    @Test
    @DisplayName("ExchangeRateRefresher should refresh and store rates from WireMock")
    void shouldRefreshRatesUsingWireMock() {
        // Arrange
        wireMockServer.stubFor(get(urlPathEqualTo("/price"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "USD/RUB": { "price": "84.50000" },
                                    "EUR/RUB": { "price": "92.00000" },
                                    "BYN/RUB": { "price": "28.50000" }
                                }
                                """)));

        // Act
        refresher.refreshRates();
        Map<String, BigDecimal> rates = exchangeRateService.getRates();

        // Assert
        assertThat(rates).isNotNull();
        assertThat(rates.get("USD")).isEqualByComparingTo("84.50000");
        assertThat(rates.get("EUR")).isEqualByComparingTo("92.00000");
        assertThat(rates.get("BYN")).isEqualByComparingTo("28.50000");
        assertThat(rates.get("RUB")).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("ExchangeRateRefresher should handle WireMock API error responses gracefully without crashing")
    void shouldHandleWireMockApiErrorGracefully() {
        // Arrange
        wireMockServer.stubFor(get(urlPathEqualTo("/price"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": 429,
                                    "message": "API rate limit reached",
                                    "status": "error"
                                }
                                """)));

        // Act & Assert (Should not throw)
        refresher.refreshRates();
        Map<String, BigDecimal> rates = exchangeRateService.getRates();
        assertThat(rates).isNotEmpty();
        assertThat(rates.get("RUB")).isEqualByComparingTo("1");
    }
}
