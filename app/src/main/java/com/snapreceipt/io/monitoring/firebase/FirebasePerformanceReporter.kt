package com.snapreceipt.io.monitoring.firebase

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.snapreceipt.io.monitoring.MonitoringConfig
import com.skybound.space.core.monitoring.PerformanceReporter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Performance 上报适配器。
 *
 * 负责把页面加载、接口请求等耗时转换为统一命名的 trace，
 * 这样控制台上可以直接按前缀筛选不同类型的性能指标。
 */
@Singleton
class FirebasePerformanceReporter @Inject constructor() : PerformanceReporter {

    // 开关支持运行时动态控制，便于在监控异常时快速降级。
    @Volatile
    private var enabled: Boolean = true

    // 仅在真正需要上报时初始化 Firebase Performance。
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
            // trace 名称会进入 Firebase 索引，必须先做规范化和长度裁剪。
            val trace = performance?.newTrace(sanitizeTraceName(prefix, rawName)) ?: return@runCatching
            recordDuration(trace, durationMs.coerceAtLeast(0L))
        }
    }

    private fun recordDuration(trace: Trace, durationMs: Long) {
        // 这里不依赖 trace 的真实开始/结束时间，而是把业务侧统计好的耗时直接写入 metric。
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
        // Firebase trace 名称只保留小写字母、数字和下划线，其他字符统一转成 `_`。
        private val NON_TRACE_NAME_REGEX = Regex("[^a-z0-9_]")
    }
}
