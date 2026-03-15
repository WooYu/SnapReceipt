package com.snapreceipt.io.config.settings

import com.snapreceipt.io.monitoring.MonitoringConfig

/**
 * 应用设置数据模型
 * 
 * 封装所有应用级别的配置项，由 [SettingsManager] 管理持久化。
 * 所有字段都有默认值，保证在无缓存时的回退行为。
 * 
 * 配置优先级：
 * - 编译期配置（BuildConfig.DEBUG）决定初始值
 * - 运行时配置（AppSettings）可覆盖编译期配置（用于测试/调试）
 */
data class AppSettings(
    /** 是否启用深色模式 */
    val isDarkMode: Boolean = false,
    
    /** 界面语言（"English" / "简体中文" 等） */
    val language: String = "English",
    
    /** 全局字体大小（单位：sp） */
    val fontSize: Float = 14f,
    
    /** 是否启用推送通知 */
    val enableNotifications: Boolean = true,
    
    /** 是否启用崩溃报告上传 */
    val enableCrashReporting: Boolean = MonitoringConfig.Defaults.crashReportingEnabled,
    
    /** 是否启用数据分析（埋点统计） */
    val enableAnalytics: Boolean = MonitoringConfig.Defaults.analyticsEnabled,

    /** 是否启用本地诊断日志落盘 */
    val enableDiagnosticFileLogging: Boolean = MonitoringConfig.Defaults.diagnosticFileLoggingEnabled,

    /** 是否显示内部测试项（通过隐藏入口解锁） */
    val showInternalTestingOptions: Boolean = false,

    /** 内部测试项自动隐藏时间戳（毫秒，0 表示未解锁） */
    val internalTestingOptionsVisibleUntilMillis: Long = 0L,
    
    /** 应用主题名称（"Light" / "Dark" / "Auto"） */
    val appTheme: String = "Light",
    
    /** 最后一次数据同步的时间戳（毫秒） */
    val lastSyncTime: Long = 0L,
    
    /**
     * 调试日志开关（运行时可覆盖）
     * 
     * - null：跟随编译期配置（BuildConfig.DEBUG）
     * - true：强制开启调试日志（生产包也可临时启用，用于排查问题）
     * - false：强制关闭调试日志（开发包也可关闭，减少日志噪音）
     * 
     * 生效范围：
     * - LogHelper.d/i/w/e 日志输出
     * - LoggingInterceptor 运行时网络请求日志
     * - AppConfig.isDebug 标志位
     */
    val enableDebugLogging: Boolean? = null
)
