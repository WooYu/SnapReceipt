package com.skybound.space.core.di

enum class DiEnvironment {
    DEV,
    TEST,
    PROD
}

interface DependencyRegistry {
    fun <T : Any> register(type: Class<T>, provider: () -> T)
}

interface DiConfig {
    val environment: DiEnvironment
    fun register(registry: DependencyRegistry)
}
