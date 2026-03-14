package com.snapreceipt.io.monitoring

import com.snapreceipt.io.config.settings.AppSettings

object MonitoringConfig {

    object Defaults {
        const val crashReportingEnabled = true
        const val analyticsEnabled = true
        const val performanceReportingEnabled = true
        const val diagnosticFileLoggingEnabled = false
    }

    object DiagnosticLogs {
        const val logsDir = "diagnostics/logs"
        const val exportDir = "diagnostics"
        const val currentFileName = "diagnostic-current.log"
        const val rotatedFilePrefix = "diagnostic-"
        const val rotatedFileSuffix = ".log"
        const val exportZipPrefix = "diagnostics-"
        const val exportZipSuffix = ".zip"
        const val maxFileBytes = 2L * 1024L * 1024L
        const val maxRotatedFiles = 5
    }

    object Crash {
        const val logMessageMaxLength = 512
    }

    object FirebaseAnalytics {
        const val eventNamePrefix = "event"
        const val paramNamePrefix = "param"
        const val maxEventNameLength = 40
        const val maxParamNameLength = 40
    }

    object FirebasePerformance {
        const val screenLoadPrefix = "screen_load"
        const val apiCallPrefix = "api_call"
        const val durationMetricKey = "duration_ms"
        const val maxTraceNameLength = 100
    }

    const val performanceFollowsCrashReporting = true
    const val allowReleaseDiagnosticLogging = true

    fun resolveDebugLogging(baselineDebug: Boolean, settings: AppSettings): Boolean {
        return resolveDebugLogging(
            baselineDebug = baselineDebug,
            debugLoggingOverride = settings.enableDebugLogging,
            diagnosticFileLoggingEnabled = settings.enableDiagnosticFileLogging
        )
    }

    fun resolveDebugLogging(
        baselineDebug: Boolean,
        debugLoggingOverride: Boolean?,
        diagnosticFileLoggingEnabled: Boolean
    ): Boolean {
        if (allowReleaseDiagnosticLogging && diagnosticFileLoggingEnabled) {
            return true
        }
        return debugLoggingOverride ?: baselineDebug
    }

    fun resolvePerformanceEnabled(crashReportingEnabled: Boolean): Boolean {
        return if (performanceFollowsCrashReporting) {
            crashReportingEnabled
        } else {
            Defaults.performanceReportingEnabled
        }
    }
}
