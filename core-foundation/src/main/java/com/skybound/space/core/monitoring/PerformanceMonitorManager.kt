package com.skybound.space.core.monitoring

interface PerformanceReporter {
    fun setEnabled(enabled: Boolean)
    fun trackScreenLoad(screenName: String, durationMs: Long)
    fun trackApiCall(endpoint: String, durationMs: Long)
}

private object NoOpPerformanceReporter : PerformanceReporter {
    override fun setEnabled(enabled: Boolean) = Unit

    override fun trackScreenLoad(screenName: String, durationMs: Long) = Unit

    override fun trackApiCall(endpoint: String, durationMs: Long) = Unit
}

object PerformanceMonitorManager {
    @Volatile
    private var delegate: PerformanceReporter = NoOpPerformanceReporter

    fun init(delegate: PerformanceReporter?) {
        this.delegate = delegate ?: NoOpPerformanceReporter
    }

    fun setEnabled(enabled: Boolean) {
        runCatching { delegate.setEnabled(enabled) }
    }

    fun trackScreenLoad(screenName: String, durationMs: Long) {
        runCatching { delegate.trackScreenLoad(screenName, durationMs) }
    }

    fun trackApiCall(endpoint: String, durationMs: Long) {
        runCatching { delegate.trackApiCall(endpoint, durationMs) }
    }
}
