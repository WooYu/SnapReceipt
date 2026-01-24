package com.snapreceipt.io.data.network.di

import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.NetworkManager
import com.skybound.space.core.network.NetworkConfig
import com.skybound.space.core.config.AppConfig
import android.content.Context
import com.skybound.space.core.network.auth.AuthTokenStore
import com.skybound.space.core.network.auth.EncryptedAuthTokenStore
import com.skybound.space.core.network.auth.SessionManager
import com.skybound.space.core.network.interceptor.AuthInterceptor
import com.skybound.space.core.network.interceptor.AuthFailureInterceptor
import com.skybound.space.core.network.interceptor.ExportTimeoutInterceptor
import com.skybound.space.core.network.interceptor.LoggingInterceptor
import com.snapreceipt.io.data.network.auth.TokenRefreshAuthenticator
import com.snapreceipt.io.data.network.datasource.AuthRemoteDataSource
import com.snapreceipt.io.data.network.datasource.ConfigRemoteDataSource
import com.snapreceipt.io.data.network.datasource.FileRemoteDataSource
import com.snapreceipt.io.data.network.datasource.ReceiptRemoteDataSource
import com.snapreceipt.io.data.network.datasource.UploadRemoteDataSource
import com.snapreceipt.io.data.network.service.AuthApi
import com.snapreceipt.io.data.network.service.ConfigApi
import com.snapreceipt.io.data.network.service.FileApi
import com.snapreceipt.io.data.network.service.ReceiptApi
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Authenticator
import okhttp3.logging.HttpLoggingInterceptor
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
    fun provideDispatchers(): CoroutineDispatchersProvider = CoroutineDispatchersProvider.Default

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideAuthTokenStore(
        @ApplicationContext context: Context
    ): AuthTokenStore = EncryptedAuthTokenStore(context)

    @Provides
    @Singleton
    fun provideTokenRefreshAuthenticator(
        tokenStore: AuthTokenStore,
        config: NetworkConfig,
        gson: com.google.gson.Gson,
        sessionManager: SessionManager
    ): Authenticator = TokenRefreshAuthenticator(tokenStore, config, gson, sessionManager)

    @Provides
    @IntoSet
    fun provideAuthInterceptor(tokenStore: AuthTokenStore): Interceptor =
        AuthInterceptor(tokenStore)

    @Provides
    @IntoSet
    fun provideAuthFailureInterceptor(sessionManager: SessionManager): Interceptor =
        AuthFailureInterceptor(sessionManager)

    @Provides
    @IntoSet
    fun provideExportTimeoutInterceptor(config: NetworkConfig): Interceptor =
        ExportTimeoutInterceptor(timeoutSec = config.exportTimeoutSec)

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig(
        baseUrl = AppConfig.baseUrl,
        enableLogging = AppConfig.isDebug,
        defaultHeaders = mapOf(
            "Accept" to "application/json"
        ),
        exportTimeoutSec = 60
    )

    @Provides
    @Singleton
    fun provideNetworkManager(
        config: NetworkConfig,
        extraInterceptors: Set<@JvmSuppressWildcards Interceptor>,
        authenticator: Authenticator
    ): NetworkManager = NetworkManager(config, extraInterceptors.toList(), authenticator)

    @Provides
    @Singleton
    fun provideRetrofit(client: NetworkManager): Retrofit = client.retrofit

    @Provides
    @Singleton
    fun provideOkHttpClient(client: NetworkManager): OkHttpClient = client.okHttpClient

    @Provides
    @Singleton
    @UploadClient
    fun provideUploadOkHttpClient(config: NetworkConfig): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSec, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSec, TimeUnit.SECONDS)
            .apply {
                if (config.enableLogging) {
                    addInterceptor(LoggingInterceptor(HttpLoggingInterceptor.Level.HEADERS))
                }
            }
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
    fun provideConfigApi(retrofit: Retrofit): ConfigApi = retrofit.create(ConfigApi::class.java)

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
}
