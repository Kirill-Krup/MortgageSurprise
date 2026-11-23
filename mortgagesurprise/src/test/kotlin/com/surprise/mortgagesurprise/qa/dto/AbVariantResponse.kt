package com.surprise.mortgagesurprise.qa.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AbVariantResponse(
    val experimentName: String,
    val color: String
)