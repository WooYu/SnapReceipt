package com.snapreceipt.io.monitoring.firebase

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.snapreceipt.io.monitoring.MonitoringConfig
import com.skybound.space.core.monitoring.AnalyticsReporter
import com.skybound.space.core.monitoring.TrackEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Analytics 上报适配器。
 *
 * 这里除了简单转发埋点外，还负责把事件名和参数名规整成 Firebase 可接受的格式，
 * 避免因为命名不合法导致数据被静默丢弃。
 */
@Singleton
class FirebaseAnalyticsReporter @Inject constructor(
    @ApplicationContext private val context: Context
) : AnalyticsReporter {

    // 运行时可由设置页动态关闭埋点，避免频繁重建 Firebase 实例。
    @Volatile
    private var enabled: Boolean = true

    // 延迟初始化，确保在未实际使用埋点时不会过早触发 Firebase 初始化。
    private val analytics: FirebaseAnalytics? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runCatching { FirebaseAnalytics.getInstance(context) }.getOrNull()
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        runCatching { analytics?.setAnalyticsCollectionEnabled(enabled) }
    }

    override fun track(event: TrackEvent) {
        if (!enabled) return
        runCatching {
            val bundle = Bundle().apply {
                // 统一清洗参数名，降低“埋点已调用但控制台没有数据”的排查成本。
                event.attributes.forEach { (key, value) ->
                    putString(
                        sanitizeName(
                            key,
                            prefix = MonitoringConfig.FirebaseAnalytics.paramNamePrefix,
                            maxLength = MonitoringConfig.FirebaseAnalytics.maxParamNameLength
                        ),
                        value
                    )
                }
            }
            analytics?.logEvent(
                // Firebase 对事件名格式限制严格，这里在上报前做最后一道兜底。
                sanitizeName(
                    event.name,
                    prefix = MonitoringConfig.FirebaseAnalytics.eventNamePrefix,
                    maxLength = MonitoringConfig.FirebaseAnalytics.maxEventNameLength
                ),
                bundle.takeUnless { it.isEmpty }
            )
        }
    }

    private fun sanitizeName(raw: String, prefix: String, maxLength: Int): String {
        val normalized = raw
            .lowercase(Locale.US)
            .replace(NON_EVENT_NAME_REGEX, "_")
            .trim('_')
        val safeName = when {
            normalized.isBlank() -> "${prefix}_unknown"
            normalized.first().isLetter() -> normalized
            else -> "${prefix}_$normalized"
        }
        return safeName.take(maxLength)
    }

    private companion object {
        // Firebase 事件名/参数名仅允许小写字母、数字和下划线。
        private val NON_EVENT_NAME_REGEX = Regex("[^a-z0-9_]")
    }
}
