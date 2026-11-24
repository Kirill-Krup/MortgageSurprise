package com.surprise.mortgagesurprise.qa.api

import com.surprise.mortgagesurprise.qa.dto.AbVariantResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface AbTestApi {

    @GET("/api/v1/ab-test/variant")
    fun getVariant(@Query("experimentName") experimentName: String): Call<AbVariantResponse>
}