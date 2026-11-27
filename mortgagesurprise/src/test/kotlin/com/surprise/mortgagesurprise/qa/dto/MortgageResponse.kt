package com.surprise.mortgagesurprise.qa.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MortgageResponse(
    val monthlyPayment: Double? = null,
    val totalAmount: Double? = null,
    val overpayment: Double? = null
)