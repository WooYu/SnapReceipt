package com.skybound.space.core.testing

import com.skybound.space.core.network.NetworkConfig

/**
 * Pre-configured [NetworkConfig] for unit/integration tests.
 */
fun testNetworkConfig(
    baseUrl: String = "https://test.api.local",
    enableLogging: Boolean = false
): NetworkConfig = NetworkConfig(
    baseUrl = baseUrl,
    connectTimeoutSec = 5,
    readTimeoutSec = 5,
    writeTimeoutSec = 5,
    enableLogging = enableLogging
)