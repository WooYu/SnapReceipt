package com.skybound.space.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

/**
 * 代理 OkHttp 官方日志拦截器，便于在单元测试中替换。
 */
class LoggingInterceptor(
    private val level: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BODY
) : Interceptor {

    private val delegate = HttpLoggingInterceptor().apply { setLevel(level) }

    override fun intercept(chain: Interceptor.Chain): Response = delegate.intercept(chain)
}
