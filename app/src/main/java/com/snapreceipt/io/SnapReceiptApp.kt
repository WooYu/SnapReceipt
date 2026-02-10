package com.snapreceipt.io

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.snapreceipt.io.config.settings.SettingsManager
import com.snapreceipt.io.di.AppDiConfig
import com.skybound.space.core.config.AppConfig
import com.skybound.space.core.di.AppInjector
import com.skybound.space.core.di.DiEnvironment
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

    override fun onCreate() {
        super.onCreate()
        
        // ── 1. 创建 DI 配置并统一初始化 ───────────────────────
        val diConfig = AppDiConfig(this)
        val isDebugEnv = diConfig.environment == DiEnvironment.DEV
        
        // 从 DiEnvironment 统一初始化（编译期配置）
        AppConfig.init(isDebug = isDebugEnv)
        LogHelper.init(isDebug = isDebugEnv)
        
        LogHelper.i(
            "AppConfig",
            "Initialized: env=${diConfig.environment} debug=$isDebugEnv baseUrl=${AppConfig.baseUrl} version=${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
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
                // 计算最终的调试模式状态
                val finalDebugMode = settings.enableDebugLogging ?: baselineDebug
                
                // 动态更新 LogHelper 和 AppConfig
                LogHelper.updateDebugMode(finalDebugMode)
                AppConfig.updateDebugMode(finalDebugMode)
                
                // 记录覆盖状态（便于排查）
                if (settings.enableDebugLogging != null) {
                    LogHelper.i(
                        "AppSettings",
                        "Debug logging overridden: baseline=$baselineDebug override=${settings.enableDebugLogging} final=$finalDebugMode"
                    )
                }
            }
        }
    }
}
