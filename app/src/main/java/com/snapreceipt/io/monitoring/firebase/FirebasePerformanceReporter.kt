package com.snapreceipt.io.monitoring.firebase

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.snapreceipt.io.monitoring.MonitoringConfig
import com.skybound.space.core.monitoring.PerformanceReporter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebasePerformanceReporter @Inject constructor() : PerformanceReporter {

    @Volatile
    private var enabled: Boolean = true

    private val performance: FirebasePerformance? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runCatching { FirebasePerformance.getInstance() }.getOrNull()
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        runCatching { performance?.setPerformanceCollectionEnabled(enabled) }
    }

    override fun trackScreenLoad(screenName: String, durationMs: Long) {
        recordDurationTrace(
            prefix = MonitoringConfig.FirebasePerformance.screenLoadPrefix,
            rawName = screenName,
            durationMs = durationMs
        )
    }

    override fun trackApiCall(endpoint: String, durationMs: Long) {
        recordDurationTrace(
            prefix = MonitoringConfig.FirebasePerformance.apiCallPrefix,
            rawName = endpoint,
            durationMs = durationMs
        )
    }

    private fun recordDurationTrace(prefix: String, rawName: String, durationMs: Long) {
        if (!enabled) return
        runCatching {
            val trace = performance?.newTrace(sanitizeTraceName(prefix, rawName)) ?: return@runCatching
            recordDuration(trace, durationMs.coerceAtLeast(0L))
        }
    }

    private fun recordDuration(trace: Trace, durationMs: Long) {
        trace.start()
        trace.putMetric(MonitoringConfig.FirebasePerformance.durationMetricKey, durationMs)
        trace.stop()
    }

    private fun sanitizeTraceName(prefix: String, rawName: String): String {
        val normalized = rawName
            .lowercase(Locale.US)
            .replace(NON_TRACE_NAME_REGEX, "_")
            .trim('_')
            .ifBlank { "unknown" }
        return "${prefix}_${normalized}".take(MonitoringConfig.FirebasePerformance.maxTraceNameLength)
    }

    private companion object {
        private val NON_TRACE_NAME_REGEX = Regex("[^a-z0-9_]")
    }
}
