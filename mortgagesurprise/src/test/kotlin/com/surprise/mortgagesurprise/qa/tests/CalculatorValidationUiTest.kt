package com.surprise.mortgagesurprise.qa.tests

import com.codeborne.selenide.Configuration
import com.surprise.mortgagesurprise.qa.pages.MortgageCalculatorPage
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Epic("Mortgage Calculator")
@Feature("UI Validation and Controls")
class CalculatorValidationUiTest {

    private val calculatorPage = MortgageCalculatorPage()

    companion object {
        @BeforeAll
        @JvmStatic
        fun setUpBrowser() {
            Configuration.browser = "chrome"
            Configuration.headless = true
        }
    }

    @Test
    @DisplayName("UI: Check calculator form controls availability")
    fun shouldDisplayCalculatorFormControls() {
        calculatorPage
            .openPage()
            .shouldHaveCalculateButtonVisible()
    }
}
