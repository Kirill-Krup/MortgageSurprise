package com.surprise.mortgagesurprise.qa.tests

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.surprise.mortgagesurprise.qa.service.AbTestApiService
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Epic("A/B Experiments")
@Feature("Variant assignment")
class AbTestApiTest {

    private val wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
    private lateinit var abTestService: AbTestApiService

    @BeforeEach
    fun setUp() {
        wireMockServer.start()
        abTestService = AbTestApiService("http://localhost:${wireMockServer.port()}")
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    @DisplayName("API: Get assigned variant for mortgage calculate button via WireMock stub")
    fun shouldReturnAssignedVariantColor() {
        wireMockServer.stubFor(
            get(urlEqualTo("/api/v1/ab-test/variant?experimentName=mortgage_calculate_button"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "experimentName": "mortgage_calculate_button",
                                "color": "blue"
                            }
                            """.trimIndent()
                        )
                )
        )

        val response = abTestService.getVariant("mortgage_calculate_button")

        assertThat(response.isSuccessful).isTrue()

        val body = response.body()
        assertThat(body).isNotNull
        assertThat(body?.experimentName).isEqualTo("mortgage_calculate_button")
        assertThat(body?.color).isEqualTo("blue")

        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/v1/ab-test/variant?experimentName=mortgage_calculate_button")))
    }
}
