package com.skybound.space.core.network.interceptor

import com.skybound.space.core.util.LogHelper
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

/**
 * 代理 OkHttp 官方日志拦截器，便于在单元测试中替换。
 */
class LoggingInterceptor(
    private val level: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BODY,
    private val tag: String = "OkHttp",
    private val redactedHeaders: Set<String> = setOf("Authorization")
) : Interceptor {

    private val delegate = HttpLoggingInterceptor { message ->
        LogHelper.d(tag, message)
    }

    init {
        delegate.level = level
        redactedHeaders.forEach { delegate.redactHeader(it) }
    }

    override fun intercept(chain: Interceptor.Chain): Response = delegate.intercept(chain)
}
