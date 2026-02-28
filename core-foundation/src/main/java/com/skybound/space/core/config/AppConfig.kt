package com.skybound.space.core.config

object AppConfig {
    const val DEFAULT_BASE_URL = "https://api.snapreceipt.io"

    @Volatile
    var baseUrl: String = DEFAULT_BASE_URL
        private set

    @Volatile
    var isDebug: Boolean = false
        private set

    fun init(isDebug: Boolean, baseUrl: String = DEFAULT_BASE_URL) {
        this.isDebug = isDebug
        this.baseUrl = normalizeBaseUrl(baseUrl)
    }

    fun updateDebugMode(enabled: Boolean) {
        isDebug = enabled
    }

    fun updateBaseUrl(url: String) {
        baseUrl = normalizeBaseUrl(url)
    }

    private fun normalizeBaseUrl(url: String): String = url.trimEnd('/')
}