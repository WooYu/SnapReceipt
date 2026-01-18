package com.snapreceipt.io.data.network.auth

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.snapreceipt.io.data.network.model.AuthTokensDto
import com.snapreceipt.io.data.network.model.RefreshRequestDto
import com.skybound.space.core.network.BaseResponse
import com.skybound.space.core.network.NetworkConfig
import com.skybound.space.core.network.auth.AuthTokenStore
import com.skybound.space.core.network.interceptor.LoggingInterceptor
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class TokenRefreshAuthenticator(
    private val tokenStore: AuthTokenStore,
    private val config: NetworkConfig,
    private val gson: Gson
) : Authenticator {

    private val refreshLock = Any()
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSec, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSec, TimeUnit.SECONDS)
            .apply {
                if (config.enableLogging) {
                    addInterceptor(LoggingInterceptor(HttpLoggingInterceptor.Level.HEADERS))
                }
            }
            .build()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val request = response.request
        val requestAuth = request.header(AUTH_HEADER) ?: return null
        val currentAccess = tokenStore.accessToken()
        if (currentAccess.isNullOrBlank()) return null

        synchronized(refreshLock) {
            val latestAccess = tokenStore.accessToken()
            val latestRefresh = tokenStore.refreshToken()
            if (latestAccess.isNullOrBlank() || latestRefresh.isNullOrBlank()) return null

            if (requestAuth != bearer(latestAccess)) {
                return request.newBuilder()
                    .header(AUTH_HEADER, bearer(latestAccess))
                    .build()
            }

            val refreshed = refreshTokens(latestRefresh) ?: return null
            tokenStore.update(refreshed.accessToken, refreshed.refreshToken)
            return request.newBuilder()
                .header(AUTH_HEADER, bearer(refreshed.accessToken))
                .build()
        }
    }

    private fun refreshTokens(refreshToken: String): AuthTokensDto? {
        val url = "${config.baseUrl.trimEnd('/')}/api/auth/refresh"
        val payload = gson.toJson(RefreshRequestDto(refreshToken))
        val body = payload.toRequestBody(JSON)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        refreshClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val raw = response.body?.string() ?: return null
            val type = object : TypeToken<BaseResponse<AuthTokensDto>>() {}.type
            val envelope: BaseResponse<AuthTokensDto> = gson.fromJson(raw, type)
            if (!envelope.isSuccess()) return null
            return envelope.data
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private fun bearer(token: String): String = "Bearer $token"

    private companion object {
        const val AUTH_HEADER = "Authorization"
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
