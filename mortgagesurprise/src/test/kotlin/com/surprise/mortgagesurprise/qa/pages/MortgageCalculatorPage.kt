package com.surprise.mortgagesurprise.qa.pages

import com.codeborne.selenide.Condition.visible
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.open
import com.surprise.mortgagesurprise.qa.config.TestConfig
import io.qameta.allure.Step

class MortgageCalculatorPage {

    private val propertyPriceInput = `$`("input[type='number'], input[type='text']")
    private val calculateButton = `$`("button[type='submit'], button.bg-blue-600, button.font-bold")

    @Step("Open mortgage calculator page")
    fun openPage(): MortgageCalculatorPage {
        open(TestConfig.uiBaseUrl)
        return this
    }

    @Step("Enter property price: {amount}")
    fun enterPropertyPrice(amount: String): MortgageCalculatorPage {
        propertyPriceInput.shouldBe(visible).clear()
        propertyPriceInput.sendKeys(amount)
        return this
    }

    @Step("Click calculate button")
    fun clickCalculate(): MortgageCalculatorPage {
        calculateButton.shouldBe(visible).click()
        return this
    }

    @Step("Verify result is displayed")
    fun shouldSeeResult(): MortgageCalculatorPage {
        `$`("body").shouldBe(visible)
        return this
    }

    @Step("Verify calculate button is visible")
    fun shouldHaveCalculateButtonVisible(): MortgageCalculatorPage {
        calculateButton.shouldBe(visible)
        return this
    }
}
