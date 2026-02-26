package com.skybound.space.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale
import java.util.UUID

/**
 * 统一注入请求头，保持幂等，可在运行时动态扩展。
 */
class DefaultHeadersInterceptor(
    private val headers: Map<String, String>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
        headers.forEach { (key, value) ->
            if (original.header(key) == null) {
                builder.header(key, value)
            }
        }
        if (original.header("X-Request-Id") == null) {
            builder.header("X-Request-Id", UUID.randomUUID().toString())
        }
        if (original.header("Accept-Language") == null) {
            val locale = Locale.getDefault()
            val tag = if (locale.country.isNotEmpty()) "${locale.language}-${locale.country}" else locale.language
            builder.header("Accept-Language", tag)
        }
        return chain.proceed(builder.build())
    }
}