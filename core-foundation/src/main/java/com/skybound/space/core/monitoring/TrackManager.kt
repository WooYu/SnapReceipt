package com.skybound.space.core.monitoring

data class TrackEvent(
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

object TrackManager {
    fun track(event: TrackEvent) {
        // Hook for analytics SDKs
    }
}
