/**
 * 应用设置和首选项管理
 * 
 * 使用 DataStore 进行类型安全的持久化存储
 * 支持异步读写和热更新
 */
package com.snapreceipt.io.config.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore 扩展
val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * 应用设置数据类
 */
data class AppSettings(
    val isDarkMode: Boolean = false,
    val language: String = "English",
    val fontSize: Float = 14f,
    val enableNotifications: Boolean = true,
    val enableCrashReporting: Boolean = true,
    val enableAnalytics: Boolean = true,
    val appTheme: String = "Light",
    val lastSyncTime: Long = 0L
)

/**
 * 设置管理器
 * 
 * 使用 DataStore 管理应用全局设置
 */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        // 定义 DataStore key
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications")
        private val CRASH_REPORTING_KEY = booleanPreferencesKey("crash_reporting")
        private val ANALYTICS_KEY = booleanPreferencesKey("analytics")
        private val APP_THEME_KEY = stringPreferencesKey("app_theme")
        private val LAST_SYNC_TIME_KEY = stringPreferencesKey("last_sync_time")
    }
    
    /**
     * 获取所有设置的 Flow
     */
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            isDarkMode = preferences[DARK_MODE_KEY] ?: false,
            language = preferences[LANGUAGE_KEY] ?: "English",
            fontSize = (preferences[FONT_SIZE_KEY] ?: "14.0").toFloatOrNull() ?: 14f,
            enableNotifications = preferences[NOTIFICATIONS_KEY] ?: true,
            enableCrashReporting = preferences[CRASH_REPORTING_KEY] ?: true,
            enableAnalytics = preferences[ANALYTICS_KEY] ?: true,
            appTheme = preferences[APP_THEME_KEY] ?: "Light",
            lastSyncTime = (preferences[LAST_SYNC_TIME_KEY] ?: "0").toLongOrNull() ?: 0L
        )
    }
    
    /**
     * 设置深色模式
     */
    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDarkMode
        }
    }
    
    /**
     * 设置语言
     */
    suspend fun setLanguage(language: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }
    
    /**
     * 设置字体大小
     */
    suspend fun setFontSize(size: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = size.toString()
        }
    }
    
    /**
     * 设置通知开关
     */
    suspend fun setNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }
    
    /**
     * 设置崩溃报告开关
     */
    suspend fun setCrashReporting(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[CRASH_REPORTING_KEY] = enabled
        }
    }
    
    /**
     * 设置分析开关
     */
    suspend fun setAnalytics(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[ANALYTICS_KEY] = enabled
        }
    }
    
    /**
     * 设置应用主题
     */
    suspend fun setAppTheme(theme: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[APP_THEME_KEY] = theme
        }
    }
    
    /**
     * 更新最后同步时间
     */
    suspend fun updateLastSyncTime() {
        context.settingsDataStore.edit { preferences ->
            preferences[LAST_SYNC_TIME_KEY] = System.currentTimeMillis().toString()
        }
    }
    
    /**
     * 重置所有设置为默认值
     */
    suspend fun resetAllSettings() {
        context.settingsDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

/**
 * 用户偏好管理
 * 
 * 处理用户特定的偏好设置
 */
@Singleton
class UserPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private val LAST_OPENED_KEY = stringPreferencesKey("last_opened")
        private val FAVORITE_RECEIPTS_KEY = stringPreferencesKey("favorite_receipts")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
    }
    
    /**
     * 获取最后打开的收据 ID
     */
    fun getLastOpenedReceipt(): Flow<String?> =
        context.settingsDataStore.data.map { preferences ->
            preferences[LAST_OPENED_KEY]
        }
    
    /**
     * 设置最后打开的收据 ID
     */
    suspend fun setLastOpenedReceipt(receiptId: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[LAST_OPENED_KEY] = receiptId
        }
    }
    
    /**
     * 获取用户 ID
     */
    fun getUserId(): Flow<String?> =
        context.settingsDataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }
    
    /**
     * 设置用户 ID
     */
    suspend fun setUserId(userId: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }
    
    /**
     * 获取认证令牌
     */
    fun getAuthToken(): Flow<String?> =
        context.settingsDataStore.data.map { preferences ->
            preferences[AUTH_TOKEN_KEY]
        }
    
    /**
     * 设置认证令牌
     */
    suspend fun setAuthToken(token: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }
    
    /**
     * 清除用户认证数据
     */
    suspend fun clearAuthData() {
        context.settingsDataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
            preferences.remove(AUTH_TOKEN_KEY)
        }
    }
}
