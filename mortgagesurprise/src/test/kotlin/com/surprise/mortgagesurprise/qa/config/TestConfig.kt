package com.surprise.mortgagesurprise.qa.config

object TestConfig {
    val baseUrl: String = System.getProperty("base.url") ?: "http://localhost:8089"
    val uiBaseUrl: String = System.getProperty("ui.base.url") ?: "http://localhost:5173"
    const val WIREMOCK_PORT: Int = 8089
    const val TIMEOUT_SECONDS: Long = 10
}
