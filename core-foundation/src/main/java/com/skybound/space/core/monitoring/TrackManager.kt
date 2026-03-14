package com.skybound.space.core.monitoring

data class TrackEvent(
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

interface AnalyticsReporter {
    fun setEnabled(enabled: Boolean)
    fun track(event: TrackEvent)
}

private object NoOpAnalyticsReporter : AnalyticsReporter {
    override fun setEnabled(enabled: Boolean) = Unit

    override fun track(event: TrackEvent) = Unit
}

object TrackManager {
    @Volatile
    private var delegate: AnalyticsReporter = NoOpAnalyticsReporter

    fun init(delegate: AnalyticsReporter?) {
        this.delegate = delegate ?: NoOpAnalyticsReporter
    }

    fun setEnabled(enabled: Boolean) {
        runCatching { delegate.setEnabled(enabled) }
    }

    fun track(event: TrackEvent) {
        runCatching { delegate.track(event) }
    }

    fun track(name: String, attributes: Map<String, String> = emptyMap()) {
        track(TrackEvent(name, attributes))
    }
}
