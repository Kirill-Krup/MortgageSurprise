package com.surprise.mortgagesurprise.qa.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalRateResponse(
    val currency: String = "",
    val rate: Double = 0.0
)

