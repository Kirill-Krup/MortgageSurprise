package com.surprise.mortgagesurprise.qa.api

import com.surprise.mortgagesurprise.qa.dto.MortgageRequest
import com.surprise.mortgagesurprise.qa.dto.MortgageResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface CalculatorApi {

    @POST("/api/v1/calculator/calculate")
    fun calculate(@Body request: MortgageRequest): Call<MortgageResponse>
}