package com.snapreceipt.io.monitoring

import com.snapreceipt.io.config.settings.AppSettings

data class MonitoringSettingsSnapshot(
    val crashReportingEnabled: Boolean = MonitoringConfig.Defaults.crashReportingEnabled,
    val analyticsEnabled: Boolean = MonitoringConfig.Defaults.analyticsEnabled,
    val diagnosticFileLoggingEnabled: Boolean = MonitoringConfig.Defaults.diagnosticFileLoggingEnabled,
    val debugLoggingOverride: Boolean? = null
) {
    fun toAppSettings(): AppSettings {
        return AppSettings(
            enableCrashReporting = crashReportingEnabled,
            enableAnalytics = analyticsEnabled,
            enableDiagnosticFileLogging = diagnosticFileLoggingEnabled,
            enableDebugLogging = debugLoggingOverride
        )
    }
}
