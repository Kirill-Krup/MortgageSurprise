package com.surprise.mortgagesurprise.qa.tests

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.surprise.mortgagesurprise.qa.dto.MortgageRequest
import com.surprise.mortgagesurprise.qa.service.CalculatorApiService
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Epic("Mortgage calculator")
@Feature("Calculate mortgage")
class MortgageCalculationApiTest {

    private val wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
    private lateinit var calculatorService: CalculatorApiService

    @BeforeEach
    fun setUp() {
        wireMockServer.start()
        calculatorService = CalculatorApiService("http://localhost:${wireMockServer.port()}")
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    @DisplayName("API: Successfully calculate mortgage via WireMock stub")
    fun shouldCalculateMortgage() {
        wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/calculator/calculate"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "monthlyPayment": 55054.31,
                                "totalAmount": 13213034.4,
                                "overpayment": 8213034.4
                            }
                            """.trimIndent()
                        )
                )
        )

        val request = MortgageRequest(
            propertyValue = 5_000_000.0,
            initialPayment = 1_000_000.0,
            termYears = 20,
            interestRate = 12.0
        )

        val response = calculatorService.calculate(request)

        assertThat(response.isSuccessful).isTrue()

        val result = response.body()
        assertThat(result).isNotNull
        assertThat(result?.monthlyPayment).isEqualTo(55054.31)
        assertThat(result?.totalAmount).isEqualTo(13213034.4)

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/api/v1/calculator/calculate")))
    }
}
