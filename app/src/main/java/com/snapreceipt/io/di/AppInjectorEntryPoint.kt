package com.snapreceipt.io.di

import com.skybound.space.core.config.ConfigManager
import com.skybound.space.core.config.FeatureSwitchHelper
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.navigation.AppRouter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * App 依赖注入入口点
 * 
 * 职责：
 * - 为**非 Hilt 管理的类**提供依赖获取途径
 * - 允许在 Application.onCreate()、自定义 View、工具类等场景获取单例
 * 
 * 典型使用场景：
 * 1. **Application.onCreate()**：初始化 `AppInjector` 时需要从 Hilt 获取依赖
 * 2. **自定义 View**：View 不能用 @Inject 注入，只能通过 EntryPoint 获取
 * 3. **静态工具类**：需要访问单例但无法被 Hilt 管理
 * 
 * 使用方式：
 * ```kotlin
 * val entryPoint = EntryPointAccessors.fromApplication(
 *     application,
 *     AppInjectorEntryPoint::class.java
 * )
 * val router = entryPoint.appRouter()
 * val config = entryPoint.configManager()
 * ```
 * 
 * 注意事项：
 * - EntryPoint 仅用于**无法通过构造函数注入**的边缘场景
 * - ViewModel / Repository / UseCase 应使用 @Inject 构造函数注入，不应使用 EntryPoint
 * - 过度使用 EntryPoint 会导致依赖关系隐晦，降低代码可测试性
 * 
 * 架构说明：
 * - @InstallIn(SingletonComponent::class)：绑定到应用级单例作用域
 * - 返回的实例由 [AppFoundationModule] 提供
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppInjectorEntryPoint {
    
    /** 获取应用路由器，用于页面跳转和 Deep Link 解析 */
    fun appRouter(): AppRouter
    
    /** 获取远程配置管理器 */
    fun configManager(): ConfigManager
    
    /** 获取特性开关助手，用于 A/B 测试和灰度发布 */
    fun featureSwitchHelper(): FeatureSwitchHelper
    
    /** 获取协程调度器提供者，统一管理线程切换 */
    fun dispatchers(): CoroutineDispatchersProvider
}
