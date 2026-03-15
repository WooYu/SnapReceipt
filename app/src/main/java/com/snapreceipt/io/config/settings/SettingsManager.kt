package com.snapreceipt.io.config.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit as editSharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.snapreceipt.io.monitoring.MonitoringConfig
import com.snapreceipt.io.monitoring.MonitoringSettingsSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore 扩展属性。
 * 
 * 为 Context 添加 settingsDataStore 属性，使用单例模式确保全局只有一个实例。
 * DataStore 文件位于 `/data/data/{package}/files/datastore/settings.preferences_pb`
 */
val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * 应用设置管理器
 * 
 * 职责：
 * - 管理应用全局设置（主题、语言、字体、开关等）
 * - 基于 DataStore 提供类型安全的持久化存储
 * - 通过 Flow 提供响应式读取，支持 UI 热更新
 * 
 * 技术选型：
 * - DataStore（替代 SharedPreferences）：支持协程、类型安全、无阻塞主线程
 * - Flow：响应式数据流，UI 可自动订阅变化
 * 
 * 注意事项：
 * - 所有写操作（setXxx）都是 suspend 函数，需在协程中调用
 * - settings Flow 在 UI 层用 `collectAsState` / `collect` 订阅
 * 
 * @param context Application Context，由 Hilt 自动注入
 */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        // DataStore key 定义（类型安全）
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications")
        private val CRASH_REPORTING_KEY = booleanPreferencesKey("crash_reporting")
        private val ANALYTICS_KEY = booleanPreferencesKey("analytics")
        private val DIAGNOSTIC_FILE_LOGGING_KEY = booleanPreferencesKey("diagnostic_file_logging")
        private val INTERNAL_TESTING_OPTIONS_ENABLED_KEY = booleanPreferencesKey("internal_testing_options_enabled")
        private val APP_THEME_KEY = stringPreferencesKey("app_theme")
        private val LAST_SYNC_TIME_KEY = stringPreferencesKey("last_sync_time")
        private val DEBUG_LOGGING_KEY = stringPreferencesKey("debug_logging")  // "true" / "false" / null
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private const val MONITORING_SNAPSHOT_PREFS = "monitoring_settings_snapshot"
        private const val SNAPSHOT_CRASH_REPORTING_KEY = "crash_reporting"
        private const val SNAPSHOT_ANALYTICS_KEY = "analytics"
        private const val SNAPSHOT_DIAGNOSTIC_FILE_LOGGING_KEY = "diagnostic_file_logging"
        private const val SNAPSHOT_DEBUG_LOGGING_KEY = "debug_logging"
    }

    private val monitoringSnapshotPrefs: SharedPreferences by lazy(LazyThreadSafetyMode.NONE) {
        context.getSharedPreferences(MONITORING_SNAPSHOT_PREFS, Context.MODE_PRIVATE)
    }
    
    /**
     * 应用设置的响应式数据流。
     * 
     * UI 层订阅此 Flow，当任何设置变更时自动收到最新值。
     * 
     * 使用示例：
     * ```kotlin
     * viewLifecycleOwner.lifecycleScope.launch {
     *     settingsManager.settings.collect { settings ->
     *         applyTheme(settings.isDarkMode)
     *         updateFontSize(settings.fontSize)
     *     }
     * }
     * ```
     */
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            isDarkMode = preferences[DARK_MODE_KEY] ?: false,
            language = preferences[LANGUAGE_KEY] ?: "English",
            fontSize = (preferences[FONT_SIZE_KEY] ?: "14.0").toFloatOrNull() ?: 14f,
            enableNotifications = preferences[NOTIFICATIONS_KEY] ?: true,
            enableCrashReporting = preferences[CRASH_REPORTING_KEY]
                ?: MonitoringConfig.Defaults.crashReportingEnabled,
            enableAnalytics = preferences[ANALYTICS_KEY]
                ?: MonitoringConfig.Defaults.analyticsEnabled,
            enableDiagnosticFileLogging = preferences[DIAGNOSTIC_FILE_LOGGING_KEY]
                ?: MonitoringConfig.Defaults.diagnosticFileLoggingEnabled,
            showInternalTestingOptions = preferences[INTERNAL_TESTING_OPTIONS_ENABLED_KEY] ?: false,
            appTheme = preferences[APP_THEME_KEY] ?: "Light",
            lastSyncTime = (preferences[LAST_SYNC_TIME_KEY] ?: "0").toLongOrNull() ?: 0L,
            enableDebugLogging = preferences[DEBUG_LOGGING_KEY]?.toBooleanStrictOrNull()
        )
    }

    fun readMonitoringSnapshot(): MonitoringSettingsSnapshot {
        return MonitoringSettingsSnapshot(
            crashReportingEnabled = monitoringSnapshotPrefs.getBoolean(
                SNAPSHOT_CRASH_REPORTING_KEY,
                MonitoringConfig.Defaults.crashReportingEnabled
            ),
            analyticsEnabled = monitoringSnapshotPrefs.getBoolean(
                SNAPSHOT_ANALYTICS_KEY,
                MonitoringConfig.Defaults.analyticsEnabled
            ),
            diagnosticFileLoggingEnabled = monitoringSnapshotPrefs.getBoolean(
                SNAPSHOT_DIAGNOSTIC_FILE_LOGGING_KEY,
                MonitoringConfig.Defaults.diagnosticFileLoggingEnabled
            ),
            debugLoggingOverride = monitoringSnapshotPrefs
                .getString(SNAPSHOT_DEBUG_LOGGING_KEY, null)
                ?.toBooleanStrictOrNull()
        )
    }

    fun syncMonitoringSnapshot(settings: AppSettings) {
        updateMonitoringSnapshot {
            putBoolean(SNAPSHOT_CRASH_REPORTING_KEY, settings.enableCrashReporting)
            putBoolean(SNAPSHOT_ANALYTICS_KEY, settings.enableAnalytics)
            putBoolean(
                SNAPSHOT_DIAGNOSTIC_FILE_LOGGING_KEY,
                settings.enableDiagnosticFileLogging
            )
            if (settings.enableDebugLogging == null) {
                remove(SNAPSHOT_DEBUG_LOGGING_KEY)
            } else {
                putString(SNAPSHOT_DEBUG_LOGGING_KEY, settings.enableDebugLogging.toString())
            }
        }
    }
    
    // ── 设置更新方法（所有写操作都是 suspend 函数） ────────────
    
    /** 设置深色模式开关 */
    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDarkMode
        }
    }
    
    /** 设置界面语言 */
    suspend fun setLanguage(language: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }
    
    /** 设置全局字体大小（单位：sp） */
    suspend fun setFontSize(size: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = size.toString()
        }
    }
    
    /** 设置推送通知开关 */
    suspend fun setNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }
    
    /** 设置崩溃报告上传开关 */
    suspend fun setCrashReporting(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[CRASH_REPORTING_KEY] = enabled
        }
        updateMonitoringSnapshot {
            putBoolean(SNAPSHOT_CRASH_REPORTING_KEY, enabled)
        }
    }
    
    /** 设置数据分析（埋点）开关 */
    suspend fun setAnalytics(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[ANALYTICS_KEY] = enabled
        }
        updateMonitoringSnapshot {
            putBoolean(SNAPSHOT_ANALYTICS_KEY, enabled)
        }
    }

    /** 设置本地诊断日志落盘开关 */
    suspend fun setDiagnosticFileLogging(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[DIAGNOSTIC_FILE_LOGGING_KEY] = enabled
        }
        updateMonitoringSnapshot {
            putBoolean(SNAPSHOT_DIAGNOSTIC_FILE_LOGGING_KEY, enabled)
        }
    }

    /** 设置是否显示内部测试项 */
    suspend fun setInternalTestingOptionsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[INTERNAL_TESTING_OPTIONS_ENABLED_KEY] = enabled
        }
    }
    
    /** 设置应用主题（"Light" / "Dark" / "Auto"） */
    suspend fun setAppTheme(theme: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[APP_THEME_KEY] = theme
        }
    }
    
    /** 更新最后同步时间为当前时刻 */
    suspend fun updateLastSyncTime() {
        context.settingsDataStore.edit { preferences ->
            preferences[LAST_SYNC_TIME_KEY] = System.currentTimeMillis().toString()
        }
    }
    
    /**
     * 设置调试日志开关（运行时覆盖）
     * 
     * @param enabled null = 恢复跟随编译期配置，true/false = 强制开启/关闭
     * 
     * 使用场景：
     * - 生产包临时开启日志排查问题（测试人员/客服）
     * - 开发包关闭日志减少噪音（大量日志影响性能时）
     */
    suspend fun setDebugLogging(enabled: Boolean?) {
        context.settingsDataStore.edit { preferences ->
            if (enabled == null) {
                preferences.remove(DEBUG_LOGGING_KEY)
            } else {
                preferences[DEBUG_LOGGING_KEY] = enabled.toString()
            }
        }
        updateMonitoringSnapshot {
            if (enabled == null) {
                remove(SNAPSHOT_DEBUG_LOGGING_KEY)
            } else {
                putString(SNAPSHOT_DEBUG_LOGGING_KEY, enabled.toString())
            }
        }
    }

    /** 设置是否已完成首启引导页 */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    /** 查询是否已完成首启引导页（默认 false） */
    suspend fun isOnboardingCompleted(): Boolean {
        return context.settingsDataStore.data.first()[ONBOARDING_COMPLETED_KEY] ?: false
    }
    
    /** 重置所有设置为默认值（清空 DataStore） */
    suspend fun resetAllSettings() {
        context.settingsDataStore.edit { preferences ->
            preferences.clear()
        }
        updateMonitoringSnapshot {
            clear()
        }
    }

    private fun updateMonitoringSnapshot(
        update: SharedPreferences.Editor.() -> Unit
    ) {
        monitoringSnapshotPrefs.editSharedPreferences(commit = true, action = update)
    }
}
