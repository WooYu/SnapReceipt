package com.skybound.space.core.config

object AppConfig {
    const val DEFAULT_BASE_URL = "https://api.snapreceipt.io/"

    @Volatile
    var baseUrl: String = DEFAULT_BASE_URL
        private set

    @Volatile
    var isDebug: Boolean = false
        private set

    fun init(isDebug: Boolean, baseUrl: String = DEFAULT_BASE_URL) {
        this.isDebug = isDebug
        this.baseUrl = baseUrl
    }
}
