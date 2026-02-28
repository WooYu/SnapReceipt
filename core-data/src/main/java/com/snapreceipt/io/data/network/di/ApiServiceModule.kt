package com.snapreceipt.io.data.network.di

import com.snapreceipt.io.data.network.service.AuthApi
import com.snapreceipt.io.data.network.service.ConfigApi
import com.snapreceipt.io.data.network.service.FileApi
import com.snapreceipt.io.data.network.service.ReceiptApi
import com.snapreceipt.io.data.network.service.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiServiceModule {

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideFileApi(retrofit: Retrofit): FileApi = retrofit.create(FileApi::class.java)

    @Provides
    @Singleton
    fun provideReceiptApi(retrofit: Retrofit): ReceiptApi = retrofit.create(ReceiptApi::class.java)

    @Provides
    @Singleton
    fun provideConfigApi(retrofit: Retrofit): ConfigApi = retrofit.create(ConfigApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)
}