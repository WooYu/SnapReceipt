package com.skybound.space.core

import com.skybound.space.core.config.AppConfig
import com.skybound.space.core.monitoring.AnalyticsReporter
import com.skybound.space.core.monitoring.CrashReporter
import com.skybound.space.core.monitoring.ExceptionMonitorManager
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
 *     crashReporter = MyCrashReporter()
 *     performanceReporter = MyPerfReporter()
 *     analyticsReporter = MyAnalyticsReporter()
 * }
 * ```
 */
object CoreFoundation {

    fun init(block: Builder.() -> Unit) {
        val config = Builder().apply(block)
        AppConfig.init(isDebug = config.isDebug, baseUrl = config.baseUrl)
        LogHelper.init(isDebug = config.isDebug)
        LogHelper.messageTransformer = config.logMessageTransformer
        LogHelper.reporter = config.logReporter
        ExceptionMonitorManager.init(config.crashReporter)
        PerformanceMonitorManager.init(config.performanceReporter)
        TrackManager.init(config.analyticsReporter)
    }

    class Builder {
        var isDebug: Boolean = false
        var baseUrl: String = AppConfig.DEFAULT_BASE_URL
        var crashReporter: CrashReporter? = null
        var performanceReporter: PerformanceReporter? = null
        var analyticsReporter: AnalyticsReporter? = null
        var logMessageTransformer: ((String) -> String)? = null
        var logReporter: ((level: String, tag: String, message: String, throwable: Throwable?) -> Unit)? = null
    }
}
