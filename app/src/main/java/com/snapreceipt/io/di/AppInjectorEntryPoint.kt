package com.snapreceipt.io.di

import com.skybound.space.core.config.ConfigManager
import com.skybound.space.core.config.FeatureSwitchHelper
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.navigation.AppRouter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppInjectorEntryPoint {
    fun appRouter(): AppRouter
    fun configManager(): ConfigManager
    fun featureSwitchHelper(): FeatureSwitchHelper
    fun dispatchers(): CoroutineDispatchersProvider
}
