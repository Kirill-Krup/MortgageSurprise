package com.surprise.mortgagesurprise.qa.service

import com.surprise.mortgagesurprise.qa.api.AbTestApi
import com.surprise.mortgagesurprise.qa.client.OkHttpClientProvider
import com.surprise.mortgagesurprise.qa.client.RetrofitProvider
import com.surprise.mortgagesurprise.qa.dto.AbVariantResponse
import io.qameta.allure.Step
import retrofit2.Response

class AbTestApiService(
    private val api: AbTestApi = RetrofitProvider.createApi()
) {

    constructor(baseUrl: String) : this(
        RetrofitProvider.createRetrofit(
            OkHttpClientProvider.createClient(),
            baseUrl
        ).create(AbTestApi::class.java)
    )

    @Step("Request A/B variant for experiment: {experimentName}")
    fun getVariant(experimentName: String): Response<AbVariantResponse> {
        return api.getVariant(experimentName).execute()
    }
}
