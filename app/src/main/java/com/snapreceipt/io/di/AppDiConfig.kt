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

class AppDiConfig(
    private val application: Application
) : DiConfig {
    override val environment: DiEnvironment = if (BuildConfig.DEBUG) {
        DiEnvironment.DEV
    } else {
        DiEnvironment.PROD
    }

    override fun register(registry: DependencyRegistry) {
        val entryPoint = EntryPointAccessors.fromApplication(
            application,
            AppInjectorEntryPoint::class.java
        )
        registry.register(AppRouter::class.java) { entryPoint.appRouter() }
        registry.register(ConfigManager::class.java) { entryPoint.configManager() }
        registry.register(FeatureSwitchHelper::class.java) { entryPoint.featureSwitchHelper() }
        registry.register(CoroutineDispatchersProvider::class.java) { entryPoint.dispatchers() }
    }
}
