package com.snapreceipt.io.monitoring

import com.snapreceipt.io.config.settings.AppSettings

/**
 * 监控设置快照。
 *
 * 用于在应用冷启动、Hilt 尚未完成依赖图构建之前，快速恢复上一次生效的监控开关，
 * 避免首屏阶段出现“日志/监控开关还没同步好”的短暂不一致。
 */
data class MonitoringSettingsSnapshot(
    val crashReportingEnabled: Boolean = MonitoringConfig.Defaults.crashReportingEnabled,
    val analyticsEnabled: Boolean = MonitoringConfig.Defaults.analyticsEnabled,
    val diagnosticFileLoggingEnabled: Boolean = MonitoringConfig.Defaults.diagnosticFileLoggingEnabled,
    val debugLoggingOverride: Boolean? = null
) {
    // 启动期需要复用 AppSettings 的解析逻辑，因此先转成统一设置模型再应用。
    fun toAppSettings(): AppSettings {
        return AppSettings(
            enableCrashReporting = crashReportingEnabled,
            enableAnalytics = analyticsEnabled,
            enableDiagnosticFileLogging = diagnosticFileLoggingEnabled,
            enableDebugLogging = debugLoggingOverride
        )
    }
}
