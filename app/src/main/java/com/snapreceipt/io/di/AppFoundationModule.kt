package com.snapreceipt.io.di

import android.content.Context
import com.skybound.space.core.config.ConfigManager
import com.skybound.space.core.config.FeatureSwitchHelper
import com.skybound.space.core.navigation.AppRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App Foundation 依赖注入模块
 * 
 * 职责：
 * - 为 App 层提供 `core-foundation` 的核心依赖单例
 * - 通过 Hilt 的 @Provides 方法创建并管理生命周期
 * 
 * 安装范围：
 * - @InstallIn(SingletonComponent::class)：所有实例都是应用级单例
 * - 从 Application.onCreate() 开始存活，直到进程结束
 * 
 * 提供的依赖：
 * - [ConfigManager]：远程配置管理（从服务端拉取开关、参数等）
 * - [FeatureSwitchHelper]：特性开关（A/B 测试、灰度发布、降级开关）
 * - [AppRouter]：应用路由器（统一管理页面跳转、Deep Link 解析）
 */
@Module
@InstallIn(SingletonComponent::class)
object AppFoundationModule {
    
    /**
     * 提供配置管理器单例。
     * 
     * [ConfigManager] 负责：
     * - 从服务端拉取远程配置（JSON 格式）
     * - 本地缓存配置（避免每次启动都请求）
     * - 提供配置读取 API（getInt / getString / getBoolean）
     */
    @Provides
    @Singleton
    fun provideConfigManager(): ConfigManager = ConfigManager()

    /**
     * 提供特性开关助手单例。
     * 
     * [FeatureSwitchHelper] 基于 [ConfigManager] 实现特性开关：
     * - 支持远程动态控制特性开启/关闭（无需发版）
     * - 用于 A/B 测试、灰度发布、紧急降级等场景
     * 
     * 使用示例：
     * ```kotlin
     * if (featureSwitchHelper.isEnabled("new_ocr_engine")) {
     *     useNewOcrEngine()
     * } else {
     *     useOldOcrEngine()
     * }
     * ```
     */
    @Provides
    @Singleton
    fun provideFeatureSwitchHelper(configManager: ConfigManager): FeatureSwitchHelper {
        return FeatureSwitchHelper(configManager)
    }

    /**
     * 提供应用路由器单例。
     * 
     * [AppRouter] 统一管理页面跳转：
     * - 路由表映射：字符串路径 → Activity/Fragment
     * - Deep Link 解析：将 URL schema 转换为页面跳转
     * - 跨模块导航：core 层可通过路由触发 App 层页面（如登录过期跳转）
     * 
     * 使用示例：
     * ```kotlin
     * appRouter.navigate("/login")
     * appRouter.navigate("/receipt/detail?id=123")
     * ```
     */
    @Provides
    @Singleton
    fun provideAppRouter(
        @ApplicationContext context: Context
    ): AppRouter = AppRouter(context)
}
