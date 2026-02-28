package com.skybound.space.core

import com.skybound.space.core.config.AppConfig
import com.skybound.space.core.monitoring.AnalyticsTracker
import com.skybound.space.core.monitoring.ExceptionMonitorManager
import com.skybound.space.core.monitoring.ExceptionReporter
import com.skybound.space.core.monitoring.PerformanceMonitorManager
import com.skybound.space.core.monitoring.PerformanceReporter
import com.skybound.space.core.monitoring.TrackManager
import com.skybound.space.core.util.LogHelper

/**
 * Unified initialization entry point for core-foundation.
 *
 * Usage in Application.onCreate():
 * ```kotlin
 * CoreFoundation.init {
 *     isDebug = BuildConfig.DEBUG
 *     baseUrl = "https://api.myapp.com"
 *     exceptionReporter = MyCrashReporter()
 *     performanceReporter = MyPerfReporter()
 *     analyticsTracker = MyAnalyticsTracker()
 * }
 * ```
 */
object CoreFoundation {

    fun init(block: Builder.() -> Unit) {
        val config = Builder().apply(block)
        AppConfig.init(isDebug = config.isDebug, baseUrl = config.baseUrl)
        LogHelper.init(isDebug = config.isDebug)
        config.logMessageTransformer?.let { LogHelper.messageTransformer = it }
        config.logReporter?.let { LogHelper.reporter = it }
        config.exceptionReporter?.let { ExceptionMonitorManager.reporter = it }
        config.performanceReporter?.let { PerformanceMonitorManager.reporter = it }
        config.analyticsTracker?.let { TrackManager.tracker = it }
    }

    class Builder {
        var isDebug: Boolean = false
        var baseUrl: String = AppConfig.DEFAULT_BASE_URL
        var exceptionReporter: ExceptionReporter? = null
        var performanceReporter: PerformanceReporter? = null
        var analyticsTracker: AnalyticsTracker? = null
        var logMessageTransformer: ((String) -> String)? = null
        var logReporter: ((level: String, tag: String, message: String, throwable: Throwable?) -> Unit)? = null
    }
}