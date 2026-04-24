package com.skybound.space.core.network.interceptor

import com.skybound.space.core.network.auth.SessionManager
import com.skybound.space.core.util.LogHelper
import okhttp3.Interceptor
import okhttp3.Response

class AuthFailureInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 403) {
            LogHelper.w(
                TAG,
                "HTTP 403 captured by AuthFailureInterceptor, trigger RequireLogin path=${request.url.encodedPath}"
            )
            sessionManager.refreshTokenInvalid()
        }
        return response
    }

    private companion object {
        private const val TAG = "Auth"
    }
}
