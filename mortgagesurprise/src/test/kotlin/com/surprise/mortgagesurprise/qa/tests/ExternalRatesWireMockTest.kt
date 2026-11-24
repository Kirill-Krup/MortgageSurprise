package com.surprise.mortgagesurprise.qa.tests

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.surprise.mortgagesurprise.qa.api.ExternalRatesApi
import com.surprise.mortgagesurprise.qa.client.OkHttpClientProvider
import com.surprise.mortgagesurprise.qa.client.RetrofitProvider
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Epic("External Integrations")
@Feature("WireMock Rates Stub")
class ExternalRatesWireMockTest {

    private val wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
    private lateinit var ratesApi: ExternalRatesApi

    @BeforeEach
    fun setUp() {
        wireMockServer.start()

        val retrofit = RetrofitProvider.createRetrofit(
            okHttpClient = OkHttpClientProvider.createClient(),
            baseUrl = "http://localhost:${wireMockServer.port()}"
        )
        ratesApi = retrofit.create(ExternalRatesApi::class.java)
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    @DisplayName("WireMock + Retrofit: Fetch rate from mocked server")
    fun shouldFetchRateFromWireMockStub() {
        wireMockServer.stubFor(
            get(urlEqualTo("/external/rates/usd"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "currency": "USD",
                                "rate": 93.45
                            }
                            """.trimIndent()
                        )
                )
        )

        val response = ratesApi.getRate("usd").execute()

        assertThat(response.isSuccessful).isTrue()
        val body = response.body()
        assertThat(body).isNotNull
        assertThat(body?.currency).isEqualTo("USD")
        assertThat(body?.rate).isEqualTo(93.45)

        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/external/rates/usd")))
    }
}
