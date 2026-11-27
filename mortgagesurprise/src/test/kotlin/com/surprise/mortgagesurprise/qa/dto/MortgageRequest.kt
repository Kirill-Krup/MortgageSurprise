package com.surprise.mortgagesurprise.qa.dto

data class MortgageRequest(
    val propertyValue: Double,
    val initialPayment: Double,
    val termYears: Int,
    val interestRate: Double
)