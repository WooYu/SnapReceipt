package com.skybound.space.core.network.interceptor

import com.skybound.space.core.network.auth.AuthTokenStore
import com.skybound.space.core.util.LogHelper
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
            LogHelper.d("Auth", "Authorization skipped (No-Auth)")
            return chain.proceed(
                request.newBuilder()
                    .removeHeader("No-Auth")
                    .build()
            )
        }

        val token = tokenStore.accessToken()
        val builder = request.newBuilder()
        var authorization = request.header("Authorization")
        if (!token.isNullOrBlank() && authorization == null) {
            authorization = "Bearer $token"
            builder.header("Authorization", authorization)
        }
        val maskedAuthorization = authorization
            ?.takeIf { it.isNotBlank() }
            ?.let { header ->
                val tokenPart = header.removePrefix("Bearer ").trim()
                if (tokenPart.isBlank()) {
                    "<redacted>"
                } else {
                    "Bearer ***${tokenPart.takeLast(6)}"
                }
            } ?: "<empty>"
        LogHelper.d("Auth", "Authorization: $maskedAuthorization")
        return chain.proceed(builder.build())
    }
}
