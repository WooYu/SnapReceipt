package com.skybound.space.core.network.interceptor

import com.skybound.space.core.network.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthFailureInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 403) {
            sessionManager.refreshTokenInvalid()
        }
        return response
    }
}
