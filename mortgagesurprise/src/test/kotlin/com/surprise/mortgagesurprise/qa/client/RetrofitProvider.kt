package com.surprise.mortgagesurprise.qa.client

import com.surprise.mortgagesurprise.qa.config.TestConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

object RetrofitProvider {

    val defaultRetrofit: Retrofit by lazy {
        createRetrofit(OkHttpClientProvider.createClient(), TestConfig.baseUrl)
    }

    fun createRetrofit(okHttpClient: OkHttpClient, baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
    }

    fun <T> createApi(retrofit: Retrofit = defaultRetrofit, clazz: Class<T>): T {
        return retrofit.create(clazz)
    }

    inline fun <reified T> createApi(retrofit: Retrofit = defaultRetrofit): T {
        return retrofit.create(T::class.java)
    }
}