package com.skybound.space.core.monitoring

interface PerformanceReporter {
    fun trackScreenLoad(screenName: String, durationMs: Long)
    fun trackApiCall(endpoint: String, durationMs: Long)
}

object PerformanceMonitorManager {
    var reporter: PerformanceReporter? = null

    fun trackScreenLoad(screenName: String, durationMs: Long) {
        reporter?.trackScreenLoad(screenName, durationMs)
    }

    fun trackApiCall(endpoint: String, durationMs: Long) {
        reporter?.trackApiCall(endpoint, durationMs)
    }
}