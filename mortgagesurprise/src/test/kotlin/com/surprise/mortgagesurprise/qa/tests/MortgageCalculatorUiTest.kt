package com.surprise.mortgagesurprise.qa.tests

import com.codeborne.selenide.Configuration
import com.codeborne.selenide.logevents.SelenideLogger
import com.surprise.mortgagesurprise.qa.pages.MortgageCalculatorPage
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.selenide.AllureSelenide
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Epic("Mortgage Calculator")
@Feature("UI Calculation")
class MortgageCalculatorUiTest {

    private val calculatorPage = MortgageCalculatorPage()

    companion object {
        @BeforeAll
        @JvmStatic
        fun setUpBrowser() {
            Configuration.browser = "chrome"
            Configuration.browserSize = "1920x1080"
            Configuration.headless = true
            SelenideLogger.addListener("AllureSelenide", AllureSelenide().screenshots(true).savePageSource(false))
        }
    }

    @Test
    @DisplayName("UI: Enter parameters and calculate mortgage")
    fun shouldCalculateMortgageOnUi() {
        calculatorPage
            .openPage()
            .enterPropertyPrice("8000000")
            .clickCalculate()
            .shouldSeeResult()
    }
}
