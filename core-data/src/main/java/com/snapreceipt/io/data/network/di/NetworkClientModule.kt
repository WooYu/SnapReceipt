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
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 区分“对象存储直传客户端”和“业务接口客户端”。
 *
 * 上传客户端不需要鉴权拦截器、自动刷新 token 或默认业务头，
 * 否则预签名上传地址很容易因为多余请求头而失败。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UploadClient

/**
 * 网络依赖注入模块。
 *
 * 这里集中定义网络层的公共配置、鉴权链、Retrofit 实例以及上传专用客户端，
 * 方便排查线上问题时快速确认“请求到底经过了哪些拦截器和超时策略”。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkClientModule {

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
    // 统一收敛网络配置，避免超时和默认请求头在多个创建点发生漂移。
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig(
        baseUrl = AppConfig.baseUrl,
        enableLogging = AppConfig.isDebug,
        defaultHeaders = mapOf("Accept" to "application/json"),
        exportTimeoutSec = 60
    )

    @Provides
    @Singleton
    fun provideTokenRefreshAuthenticator(
        tokenStore: AuthTokenStore,
        config: NetworkConfig,
        gson: Gson,
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
    // 预签名上传只做原始 PUT，不挂鉴权和自动刷新逻辑，避免污染对象存储签名请求。
    fun provideUploadOkHttpClient(config: NetworkConfig): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSec, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSec, TimeUnit.SECONDS)
            .addInterceptor(LoggingInterceptor(HttpLoggingInterceptor.Level.HEADERS))
            .build()
}
