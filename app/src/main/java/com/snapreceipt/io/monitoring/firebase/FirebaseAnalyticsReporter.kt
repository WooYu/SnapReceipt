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

@Singleton
class FirebaseAnalyticsReporter @Inject constructor(
    @ApplicationContext private val context: Context
) : AnalyticsReporter {

    @Volatile
    private var enabled: Boolean = true

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
        private val NON_EVENT_NAME_REGEX = Regex("[^a-z0-9_]")
    }
}
