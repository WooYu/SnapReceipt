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

@Module
@InstallIn(SingletonComponent::class)
object AppFoundationModule {
    @Provides
    @Singleton
    fun provideConfigManager(): ConfigManager = ConfigManager()

    @Provides
    @Singleton
    fun provideFeatureSwitchHelper(configManager: ConfigManager): FeatureSwitchHelper {
        return FeatureSwitchHelper(configManager)
    }

    @Provides
    @Singleton
    fun provideAppRouter(
        @ApplicationContext context: Context
    ): AppRouter = AppRouter(context)
}
