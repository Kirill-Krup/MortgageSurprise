package com.surprise.mortgagesurprise.qa.service

import com.surprise.mortgagesurprise.qa.api.CalculatorApi
import com.surprise.mortgagesurprise.qa.client.OkHttpClientProvider
import com.surprise.mortgagesurprise.qa.client.RetrofitProvider
import com.surprise.mortgagesurprise.qa.dto.MortgageRequest
import com.surprise.mortgagesurprise.qa.dto.MortgageResponse
import io.qameta.allure.Step
import retrofit2.Response

class CalculatorApiService(
    private val api: CalculatorApi = RetrofitProvider.createApi()
) {

    constructor(baseUrl: String) : this(
        RetrofitProvider.createRetrofit(
            OkHttpClientProvider.createClient(),
            baseUrl
        ).create(CalculatorApi::class.java)
    )

    @Step("Calculate mortgage request")
    fun calculate(request: MortgageRequest): Response<MortgageResponse> {
        return api.calculate(request).execute()
    }
}
