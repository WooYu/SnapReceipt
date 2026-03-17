package com.snapreceipt.io

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.snapreceipt.io.config.settings.AppSettings
import com.snapreceipt.io.config.settings.SettingsManager
import com.snapreceipt.io.di.AppDiConfig
import com.snapreceipt.io.monitoring.MonitoringConfig
import com.snapreceipt.io.monitoring.diagnostic.DiagnosticLogManager
import com.skybound.space.core.CoreFoundation
import com.skybound.space.core.config.AppConfig
import com.skybound.space.core.di.AppInjector
import com.skybound.space.core.di.DiEnvironment
import com.skybound.space.core.monitoring.ExceptionMonitorManager
import com.skybound.space.core.monitoring.MonitoringNames
import com.skybound.space.core.monitoring.PerformanceMonitorManager
import com.skybound.space.core.monitoring.TrackManager
import com.skybound.space.core.util.LogHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SnapReceipt 应用入口
 * 
 * 职责：
 * - 初始化全局配置（日志、网络、依赖注入）
 * - 订阅运行时设置变化，动态更新系统行为
 * 
 * 初始化流程：
 * 1. 创建 DiConfig，根据 BuildConfig.DEBUG 确定环境（DEV/PROD）
 * 2. 从 DiEnvironment 统一初始化 AppConfig 和 LogHelper
 * 3. 应用依赖注入配置（注册 core 层依赖）
 * 4. 订阅 AppSettings 变化，运行时覆盖日志开关
 */
@HiltAndroidApp
class SnapReceiptApp : Application() {
    
    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var diagnosticLogManager: DiagnosticLogManager

    override fun onCreate() {
        super.onCreate()
        
        // ── 1. 创建 DI 配置并统一初始化 ───────────────────────
        val diConfig = AppDiConfig(this)
        val isDebugEnv = diConfig.environment == DiEnvironment.DEV
        val startupSettings = settingsManager.readMonitoringSnapshot().toAppSettings()
        val initialDebugMode = MonitoringConfig.resolveDebugLogging(isDebugEnv, startupSettings)

        CoreFoundation.init {
            isDebug = initialDebugMode
            logReporter = { level, tag, message, throwable ->
                routeLog(level, tag, message, throwable)
            }
        }
        applyMonitoringSettings(baselineDebug = isDebugEnv, settings = startupSettings)
        
        LogHelper.i(
            "AppConfig",
            "Initialized: env=${diConfig.environment} debug=$initialDebugMode baseUrl=${AppConfig.baseUrl} version=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
        )
        
        // ── 2. 应用依赖注入配置 ──────────────────────────────
        AppInjector.applyConfig(diConfig)
        
        // ── 3. 订阅运行时设置变化（允许动态覆盖） ─────────────
        subscribeToSettingsChanges(baselineDebug = isDebugEnv)
    }

    /**
     * 订阅 AppSettings 变化，动态更新日志开关。
     * 
     * 逻辑：
     * - enableDebugLogging = null：恢复编译期配置（baselineDebug）
     * - enableDebugLogging = true/false：覆盖编译期配置
     * 
     * 使用 ProcessLifecycleOwner 确保在应用存活期间持续监听。
     * 
     * @param baselineDebug 编译期基线配置（来自 DiEnvironment）
     */
    private fun subscribeToSettingsChanges(baselineDebug: Boolean) {
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            settingsManager.settings.collectLatest { settings ->
                settingsManager.syncMonitoringSnapshot(settings)
                applyMonitoringSettings(baselineDebug = baselineDebug, settings = settings)

                if (settings.enableDebugLogging != null || settings.enableDiagnosticFileLogging) {
                    LogHelper.i(
                        "AppSettings",
                        "Debug logging updated: baseline=$baselineDebug override=${settings.enableDebugLogging} diagnostic=${settings.enableDiagnosticFileLogging} final=${MonitoringConfig.resolveDebugLogging(baselineDebug, settings)}"
                    )
                }
            }
        }
    }

    private fun applyMonitoringSettings(baselineDebug: Boolean, settings: AppSettings) {
        val finalDebugMode = MonitoringConfig.resolveDebugLogging(baselineDebug, settings)
        diagnosticLogManager.setEnabled(settings.enableDiagnosticFileLogging)
        ExceptionMonitorManager.setEnabled(settings.enableCrashReporting)
        PerformanceMonitorManager.setEnabled(
            MonitoringConfig.resolvePerformanceEnabled(settings.enableCrashReporting)
        )
        TrackManager.setEnabled(settings.enableAnalytics)
        LogHelper.updateDebugMode(finalDebugMode)
        AppConfig.updateDebugMode(finalDebugMode)
    }

    private fun routeLog(level: String, tag: String, message: String, throwable: Throwable?) {
        diagnosticLogManager.append(level, tag, message, throwable)

        if (level != "W" && level != "E") {
            return
        }

        ExceptionMonitorManager.log(level, tag, message, throwable)
        if (throwable != null) {
            ExceptionMonitorManager.report(
                throwable,
                metadata = mapOf(
                    MonitoringNames.CrashMetadata.logLevel to level,
                    MonitoringNames.CrashMetadata.logTag to tag,
                    MonitoringNames.CrashMetadata.logMessage to
                        message.take(MonitoringConfig.Crash.logMessageMaxLength)
                )
            )
        }
    }
}
