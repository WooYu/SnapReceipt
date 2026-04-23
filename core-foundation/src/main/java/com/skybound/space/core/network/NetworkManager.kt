package com.skybound.space.core.network

import com.skybound.space.core.network.interceptor.DefaultHeadersInterceptor
import com.skybound.space.core.network.interceptor.LoggingInterceptor
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Configurable HTTP client factory.
 *
 * @param converterFactory Retrofit converter for JSON (de)serialization.
 *   Defaults to [GsonConverterFactory]. Pass a Moshi or kotlinx-serialization
 *   converter to swap the serialization library without touching core code.
 */
class NetworkManager(
    private val config: NetworkConfig,
    private val extraInterceptors: List<okhttp3.Interceptor> = emptyList(),
    private val networkInterceptors: List<okhttp3.Interceptor> = emptyList(),
    private val authenticator: Authenticator? = null,
    private val converterFactory: Converter.Factory = GsonConverterFactory.create()
) {

    val okHttpClient: OkHttpClient by lazy { buildOkHttpClient() }
    val retrofit: Retrofit by lazy { buildRetrofit(okHttpClient) }

    fun <T> create(service: Class<T>): T = retrofit.create(service)

    private fun buildOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSec, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSec, TimeUnit.SECONDS)
            .addInterceptor(DefaultHeadersInterceptor(config.defaultHeaders))
            .addInterceptor(LoggingInterceptor())
            .apply {
                extraInterceptors.forEach { addInterceptor(it) }
            }
            .apply {
                // network interceptor 必须早于 RetryAndFollowUpInterceptor 看到响应,
                // 否则 Authenticator 无法收到我们重写后的 401 状态。
                networkInterceptors.forEach { addNetworkInterceptor(it) }
            }
            .apply {
                if (authenticator != null) authenticator(authenticator)
            }
            .build()
    }

    private fun buildRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(config.baseUrl))
            .client(client)
            .addConverterFactory(converterFactory)
            .build()
    }

    private fun normalizeBaseUrl(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
}
