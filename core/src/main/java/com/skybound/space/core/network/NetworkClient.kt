package com.skybound.space.core.network

import com.skybound.space.core.network.interceptor.DefaultHeadersInterceptor
import com.skybound.space.core.network.interceptor.LoggingInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 构建可复用的 OkHttp/Retrofit 客户端。
 */
class NetworkClient(
    private val config: NetworkConfig,
    private val extraInterceptors: List<okhttp3.Interceptor> = emptyList()
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
            .apply {
                if (config.enableLogging) addInterceptor(LoggingInterceptor())
                extraInterceptors.forEach { addInterceptor(it) }
            }
            .build()
    }

    private fun buildRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(config.baseUrl))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun normalizeBaseUrl(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
}
