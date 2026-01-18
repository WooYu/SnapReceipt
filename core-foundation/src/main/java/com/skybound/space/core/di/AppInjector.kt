package com.skybound.space.core.di

import java.util.concurrent.ConcurrentHashMap

/**
 * 轻量级服务定位器，便于在不直接依赖 DI 框架的场景中获取实例。
 * 可由 Hilt/Dagger 的模块在初始化时注册依赖。
 */
object AppInjector : DependencyRegistry {
    private val registry = ConcurrentHashMap<Class<*>, () -> Any>()

    override fun <T : Any> register(type: Class<T>, provider: () -> T) {
        registry[type] = provider
    }

    fun applyConfig(config: DiConfig) {
        config.register(this)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getInstance(type: Class<T>): T {
        val provider = registry[type]
            ?: throw IllegalStateException("No provider registered for ${type.name}")
        return provider.invoke() as T
    }
}
