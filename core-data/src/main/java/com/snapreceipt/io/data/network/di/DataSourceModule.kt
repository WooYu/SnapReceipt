package com.snapreceipt.io.data.network.di

import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.snapreceipt.io.data.network.datasource.AuthRemoteDataSource
import com.snapreceipt.io.data.network.datasource.ConfigRemoteDataSource
import com.snapreceipt.io.data.network.datasource.FileRemoteDataSource
import com.snapreceipt.io.data.network.datasource.ReceiptRemoteDataSource
import com.snapreceipt.io.data.network.datasource.UploadRemoteDataSource
import com.snapreceipt.io.data.network.datasource.UserRemoteDataSource
import com.snapreceipt.io.data.network.service.AuthApi
import com.snapreceipt.io.data.network.service.ConfigApi
import com.snapreceipt.io.data.network.service.FileApi
import com.snapreceipt.io.data.network.service.ReceiptApi
import com.snapreceipt.io.data.network.service.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(
        api: AuthApi,
        dispatchers: CoroutineDispatchersProvider
    ): AuthRemoteDataSource = AuthRemoteDataSource(api, dispatchers)

    @Provides
    @Singleton
    fun provideConfigRemoteDataSource(
        api: ConfigApi,
        dispatchers: CoroutineDispatchersProvider
    ): ConfigRemoteDataSource = ConfigRemoteDataSource(api, dispatchers)

    @Provides
    @Singleton
    fun provideFileRemoteDataSource(
        api: FileApi,
        dispatchers: CoroutineDispatchersProvider
    ): FileRemoteDataSource = FileRemoteDataSource(api, dispatchers)

    @Provides
    @Singleton
    fun provideReceiptRemoteDataSource(
        api: ReceiptApi,
        dispatchers: CoroutineDispatchersProvider
    ): ReceiptRemoteDataSource = ReceiptRemoteDataSource(api, dispatchers)

    @Provides
    @Singleton
    fun provideUploadRemoteDataSource(
        @UploadClient okHttpClient: OkHttpClient,
        dispatchers: CoroutineDispatchersProvider
    ): UploadRemoteDataSource = UploadRemoteDataSource(okHttpClient, dispatchers)

    @Provides
    @Singleton
    fun provideUserRemoteDataSource(
        api: UserApi,
        dispatchers: CoroutineDispatchersProvider
    ): UserRemoteDataSource = UserRemoteDataSource(api, dispatchers)
}