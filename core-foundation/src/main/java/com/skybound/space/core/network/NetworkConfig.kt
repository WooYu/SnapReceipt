package com.skybound.space.core.network

data class NetworkConfig(
    val baseUrl: String,
    val connectTimeoutSec: Long = 10,
    val readTimeoutSec: Long = 20,
    val writeTimeoutSec: Long = 20,
    val enableLogging: Boolean = false,
    val defaultHeaders: Map<String, String> = emptyMap(),
    val exportTimeoutSec: Long = 60
)