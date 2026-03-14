package com.snapreceipt.io.monitoring

import com.snapreceipt.io.config.settings.AppSettings

/**
 * 监控系统统一配置入口。
 *
 * 这里集中维护默认开关、文件日志策略、Firebase 命名约束等“运维相关参数”，
 * 目的是让排查问题时可以直接从一个文件看清楚监控链路的行为边界。
 */
object MonitoringConfig {

    object Defaults {
        // 默认开启崩溃上报，便于线上异常及时进入告警系统。
        const val crashReportingEnabled = true
        // 默认开启埋点统计，用于分析核心功能使用情况。
        const val analyticsEnabled = true
        // 性能监控默认跟随崩溃上报开关，避免只关一半导致数据口径不一致。
        const val performanceReportingEnabled = true
        // 诊断日志默认关闭，避免日常运行产生额外本地文件开销。
        const val diagnosticFileLoggingEnabled = false
    }

    object DiagnosticLogs {
        // 诊断日志在应用私有目录下的相对路径。
        const val logsDir = "diagnostics/logs"
        // 导出 ZIP 文件的缓存目录。
        const val exportDir = "diagnostics"
        // 当前正在写入的活动日志文件名。
        const val currentFileName = "diagnostic-current.log"
        // 轮转后的历史文件统一用该前缀命名，方便筛选与清理。
        const val rotatedFilePrefix = "diagnostic-"
        // 历史日志文件统一后缀。
        const val rotatedFileSuffix = ".log"
        // 导出压缩包文件名前缀。
        const val exportZipPrefix = "diagnostics-"
        // 导出压缩包文件名后缀。
        const val exportZipSuffix = ".zip"
        // 单个日志文件达到该大小后触发轮转，防止一个文件无限膨胀。
        const val maxFileBytes = 2L * 1024L * 1024L
        // 最多保留的历史日志文件数量，超出后按时间删除最旧文件。
        const val maxRotatedFiles = 5
    }

    object Crash {
        // 崩溃日志元数据里的消息内容做截断，避免单条日志过长影响上报质量。
        const val logMessageMaxLength = 512
    }

    object FirebaseAnalytics {
        // 事件名不以字母开头时，会用此前缀兜底。
        const val eventNamePrefix = "event"
        // 参数名不合法时，用此前缀补齐。
        const val paramNamePrefix = "param"
        // Firebase Analytics 事件名长度限制。
        const val maxEventNameLength = 40
        // Firebase Analytics 参数名长度限制。
        const val maxParamNameLength = 40
    }

    object FirebasePerformance {
        // 页面加载耗时 trace 前缀，便于在控制台按类别筛选。
        const val screenLoadPrefix = "screen_load"
        // API 调用耗时 trace 前缀。
        const val apiCallPrefix = "api_call"
        // 所有耗时 trace 都复用同一个 metric key，便于报表聚合。
        const val durationMetricKey = "duration_ms"
        // Firebase Performance trace 名称最大长度限制。
        const val maxTraceNameLength = 100
    }

    // 为了简化设置项，性能监控是否开启默认跟随崩溃上报。
    const val performanceFollowsCrashReporting = true
    // 允许在 release 包里单独打开诊断日志，方便定位线上难复现问题。
    const val allowReleaseDiagnosticLogging = true

    /**
     * 计算最终是否输出调试日志。
     *
     * 优先级：
     * 1. 如果 release 包允许诊断落盘且诊断日志开关已开，则强制保留调试日志；
     * 2. 否则优先采用设置页里的显式覆盖值；
     * 3. 最后回退到编译环境基线值。
     */
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
        // 线上诊断日志落盘时必须保留调试日志，否则文件里看不到有效上下文。
        if (allowReleaseDiagnosticLogging && diagnosticFileLoggingEnabled) {
            return true
        }
        return debugLoggingOverride ?: baselineDebug
    }

    // 性能监控默认跟随崩溃上报，确保监控平台上各类数据同时开关、便于统一治理。
    fun resolvePerformanceEnabled(crashReportingEnabled: Boolean): Boolean {
        return if (performanceFollowsCrashReporting) {
            crashReportingEnabled
        } else {
            Defaults.performanceReportingEnabled
        }
    }
}
