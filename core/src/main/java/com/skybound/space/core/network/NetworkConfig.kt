package com.skybound.space.core.network

/**
 * 网络层配置，方便在不同构建环境间切换。
 */
data class NetworkConfig(
    val baseUrl: String,
    val connectTimeoutSec: Long = 10,
    val readTimeoutSec: Long = 20,
    val writeTimeoutSec: Long = 20,
    val enableLogging: Boolean = false,
    val defaultHeaders: Map<String, String> = emptyMap()
)
