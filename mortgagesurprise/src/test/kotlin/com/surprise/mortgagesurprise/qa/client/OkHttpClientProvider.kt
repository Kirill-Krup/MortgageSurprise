package com.surprise.mortgagesurprise.qa.client

import com.surprise.mortgagesurprise.qa.config.TestConfig
import io.qameta.allure.okhttp3.AllureOkHttp3
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object OkHttpClientProvider {

    fun createClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .retryOnConnectionFailure(true)
            .connectTimeout(TestConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TestConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AllureOkHttp3())
            .build()
    }
}
