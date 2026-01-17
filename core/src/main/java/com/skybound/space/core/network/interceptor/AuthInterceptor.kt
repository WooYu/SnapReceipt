package com.skybound.space.core.network.interceptor

import com.skybound.space.core.network.auth.AuthTokenStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 自动注入 Authorization 头。可通过 "No-Auth" 头跳过。
 */
class AuthInterceptor(
    private val tokenStore: AuthTokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header("No-Auth") != null) {
            return chain.proceed(
                request.newBuilder()
                    .removeHeader("No-Auth")
                    .build()
            )
        }

        val token = tokenStore.accessToken()
        val builder = request.newBuilder()
        if (!token.isNullOrBlank() && request.header("Authorization") == null) {
            builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
