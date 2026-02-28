package com.skybound.space.core.monitoring

data class TrackEvent(
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

interface AnalyticsTracker {
    fun track(event: TrackEvent)
}

object TrackManager {
    var tracker: AnalyticsTracker? = null

    fun track(event: TrackEvent) {
        tracker?.track(event)
    }

    fun track(name: String, attributes: Map<String, String> = emptyMap()) {
        track(TrackEvent(name, attributes))
    }
}