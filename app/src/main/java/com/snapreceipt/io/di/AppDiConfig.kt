package com.snapreceipt.io.di

import android.app.Application
import com.snapreceipt.io.BuildConfig
import com.skybound.space.core.config.ConfigManager
import com.skybound.space.core.config.FeatureSwitchHelper
import com.skybound.space.core.di.DiConfig
import com.skybound.space.core.di.DiEnvironment
import com.skybound.space.core.di.DependencyRegistry
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.navigation.AppRouter
import dagger.hilt.android.EntryPointAccessors

/**
 * App 层依赖注入配置
 * 
 * 职责：
 * - 实现 `core-foundation` 定义的 [DiConfig] 接口
 * - 桥接 Hilt 管理的依赖到非 Hilt 管理的模块（如 core-foundation）
 * - 根据 BuildConfig.DEBUG 自动切换开发/生产环境
 * 
 * 工作原理：
 * 1. `core-foundation` 的 `AppInjector` 在 Application.onCreate() 时调用此配置
 * 2. 通过 [AppInjectorEntryPoint] 从 Hilt 容器获取依赖
 * 3. 将依赖注册到 `DependencyRegistry`，供 core 层使用
 * 
 * 使用场景：
 * - `core-foundation` 层需要使用 App 层的 [AppRouter] 进行页面跳转
 * - `core-foundation` 层需要使用 [ConfigManager] 读取远程配置
 * - 在不引入 Hilt 的情况下，让 core 层获取单例依赖
 * 
 * @param application Application 实例，用于获取 Hilt EntryPoint
 */
class AppDiConfig(
    private val application: Application
) : DiConfig {
    
    /**
     * 运行环境标识。
     * - DEV：开发环境（启用调试日志、Mock 数据等）
     * - PROD：生产环境（关闭调试、启用混淆）
     */
    override val environment: DiEnvironment = if (BuildConfig.DEBUG) {
        DiEnvironment.DEV
    } else {
        DiEnvironment.PROD
    }

    /**
     * 注册依赖到 `core-foundation` 的依赖注册表。
     * 
     * 通过 Hilt 的 EntryPoint 机制获取已实例化的单例，
     * 然后注册到 [DependencyRegistry] 供 core 层使用。
     * 
     * @param registry core-foundation 提供的依赖注册表
     */
    override fun register(registry: DependencyRegistry) {
        // 从 Hilt 容器获取 EntryPoint（包含所有需要的单例）
        val entryPoint = EntryPointAccessors.fromApplication(
            application,
            AppInjectorEntryPoint::class.java
        )
        
        // 注册路由器（用于 core 层触发页面跳转，如登录过期跳转到登录页）
        registry.register(AppRouter::class.java) { entryPoint.appRouter() }
        
        // 注册配置管理器（用于 core 层读取远程配置）
        registry.register(ConfigManager::class.java) { entryPoint.configManager() }
        
        // 注册特性开关助手（用于 A/B 测试、灰度发布等）
        registry.register(FeatureSwitchHelper::class.java) { entryPoint.featureSwitchHelper() }
        
        // 注册协程调度器提供者（用于 core 层统一线程切换）
        registry.register(CoroutineDispatchersProvider::class.java) { entryPoint.dispatchers() }
    }
}
