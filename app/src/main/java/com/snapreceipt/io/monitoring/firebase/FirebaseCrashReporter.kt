package com.snapreceipt.io.monitoring.firebase

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.skybound.space.core.monitoring.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCrashReporter @Inject constructor() : CrashReporter {

    @Volatile
    private var enabled: Boolean = true

    private val crashlytics: FirebaseCrashlytics? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        runCatching { crashlytics?.setCrashlyticsCollectionEnabled(enabled) }
    }

    override fun recordException(throwable: Throwable, metadata: Map<String, String>) {
        if (!enabled) return
        runCatching {
            val delegate = crashlytics ?: return@runCatching
            metadata.forEach { (key, value) ->
                delegate.setCustomKey(key, value)
            }
            delegate.recordException(throwable)
        }
    }

    override fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        if (!enabled) return
        runCatching {
            val delegate = crashlytics ?: return@runCatching
            delegate.log("[$level][$tag] $message")
            throwable?.let { delegate.log(it.stackTraceToString()) }
        }
    }
}
