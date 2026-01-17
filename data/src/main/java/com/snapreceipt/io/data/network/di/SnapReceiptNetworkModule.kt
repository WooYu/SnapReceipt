package com.snapreceipt.io.data.network.di

import com.skybound.space.core.dispatcher.DispatchersProvider
import com.skybound.space.core.network.NetworkClient
import com.skybound.space.core.network.NetworkConfig
import com.skybound.space.core.network.auth.AuthTokenStore
import com.skybound.space.core.network.auth.InMemoryAuthTokenStore
import com.skybound.space.core.network.interceptor.AuthInterceptor
import com.snapreceipt.io.data.network.datasource.AuthRemoteDataSource
import com.snapreceipt.io.data.network.datasource.FileRemoteDataSource
import com.snapreceipt.io.data.network.datasource.ReceiptRemoteDataSource
import com.snapreceipt.io.data.network.datasource.UploadRemoteDataSource
import com.snapreceipt.io.data.network.service.AuthApi
import com.snapreceipt.io.data.network.service.FileApi
import com.snapreceipt.io.data.network.service.ReceiptApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UploadClient

@Module
@InstallIn(SingletonComponent::class)
object SnapReceiptNetworkModule {

    @Provides
    @Singleton
    fun provideDispatchers(): DispatchersProvider = DispatchersProvider.Default

    @Provides
    @Singleton
    fun provideAuthTokenStore(): AuthTokenStore = InMemoryAuthTokenStore()

    @Provides
    @IntoSet
    fun provideAuthInterceptor(tokenStore: AuthTokenStore): Interceptor =
        AuthInterceptor(tokenStore)

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig(
        baseUrl = "https://api.snapreceipt.io/",
        enableLogging = true,
        defaultHeaders = mapOf(
            "Accept" to "application/json"
        )
    )

    @Provides
    @Singleton
    fun provideNetworkClient(
        config: NetworkConfig,
        extraInterceptors: Set<@JvmSuppressWildcards Interceptor>
    ): NetworkClient = NetworkClient(config, extraInterceptors.toList())

    @Provides
    @Singleton
    fun provideRetrofit(client: NetworkClient): Retrofit = client.retrofit

    @Provides
    @Singleton
    fun provideOkHttpClient(client: NetworkClient): OkHttpClient = client.okHttpClient

    @Provides
    @Singleton
    @UploadClient
    fun provideUploadOkHttpClient(config: NetworkConfig): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSec, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSec, TimeUnit.SECONDS)
            .build()

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
    fun provideAuthRemoteDataSource(
        api: AuthApi,
        dispatchers: DispatchersProvider
    ): AuthRemoteDataSource = AuthRemoteDataSource(api, dispatchers)

    @Provides
    @Singleton
    fun provideFileRemoteDataSource(
        api: FileApi,
        dispatchers: DispatchersProvider
    ): FileRemoteDataSource = FileRemoteDataSource(api, dispatchers)

    @Provides
    @Singleton
    fun provideReceiptRemoteDataSource(
        api: ReceiptApi,
        dispatchers: DispatchersProvider
    ): ReceiptRemoteDataSource = ReceiptRemoteDataSource(api, dispatchers)

    @Provides
    @Singleton
    fun provideUploadRemoteDataSource(
        @UploadClient okHttpClient: OkHttpClient,
        dispatchers: DispatchersProvider
    ): UploadRemoteDataSource = UploadRemoteDataSource(okHttpClient, dispatchers)
}
