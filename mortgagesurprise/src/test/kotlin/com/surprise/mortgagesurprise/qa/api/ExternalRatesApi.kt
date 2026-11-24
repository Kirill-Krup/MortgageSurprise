package com.surprise.mortgagesurprise.qa.api

import com.surprise.mortgagesurprise.qa.dto.ExternalRateResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ExternalRatesApi {

    @GET("/external/rates/{currency}")
    fun getRate(@Path("currency") currency: String): Call<ExternalRateResponse>
}
