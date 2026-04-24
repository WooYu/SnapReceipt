package com.snapreceipt.io.data.network.auth

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.snapreceipt.io.data.network.model.auth.AuthTokensDto
import com.snapreceipt.io.data.network.model.auth.RefreshRequestDto
import com.skybound.space.core.network.BaseResponse
import com.skybound.space.core.network.NetworkConfig
import com.skybound.space.core.network.auth.AuthTokenStore
import com.skybound.space.core.network.auth.SessionManager
import com.skybound.space.core.network.interceptor.LoggingInterceptor
import com.skybound.space.core.util.LogHelper
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * 处理 access token 失效后的自动刷新逻辑。
 *
 * 核心策略：
 * 1. 同一个请求最多自动重试一次，避免 401 死循环。
 * 2. 通过 [refreshLock] 串行化刷新操作，避免并发请求同时刷新 token。
 * 3. 如果其他线程已经刷新成功，当前请求直接复用最新 token 重放。
 * 4. 如果 refresh token 也失效，则通知 [SessionManager] 进入登出/重登录流程。
 */
class TokenRefreshAuthenticator(
    private val tokenStore: AuthTokenStore,
    private val config: NetworkConfig,
    private val gson: Gson,
    private val sessionManager: SessionManager
) : Authenticator {

    private val refreshLock = Any()
    // 记录最近一次刷新失败时使用的 refresh token。N 个并发 401 同时失败时,
    // 后续线程进锁后仍看到相同 latestAccess 会再次打 /api/auth/refresh,
    // 用此标记短路避免对后端发起 N 次相同 refresh token 的重复调用。
    private var lastFailedRefreshToken: String? = null
    private var lastFailedRefreshAtMs: Long = 0L
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // 刷新 token 是兜底请求，超时要更短，避免卡住整个请求重试链路。
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(LoggingInterceptor(HttpLoggingInterceptor.Level.HEADERS))
            .build()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // OkHttp 会把 priorResponse 串起来，这里限制最多走两次，防止刷新失败后无限重放。
        if (responseCount(response) >= 2) {
            LogHelper.w(TAG, "Skip refresh: retry limit reached for ${requestPath(response.request)}")
            return null
        }

        val request = response.request
        // 仅处理本来就带鉴权头的请求，避免把无鉴权接口错误地拖进刷新流程。
        val requestAuth = request.header(AUTH_HEADER) ?: run {
            LogHelper.d(TAG, "Skip refresh: request has no Authorization header, path=${requestPath(request)}")
            return null
        }
        val currentAccess = tokenStore.accessToken()
        if (currentAccess.isNullOrBlank()) {
            LogHelper.w(TAG, "Skip refresh: access token missing in store, path=${requestPath(request)}")
            return null
        }

        synchronized(refreshLock) {
            // 进入锁后再次读取，确保拿到的是其他线程可能刚刷新过的新 token。
            val latestAccess = tokenStore.accessToken()
            val latestRefresh = tokenStore.refreshToken()
            if (latestAccess.isNullOrBlank() || latestRefresh.isNullOrBlank()) {
                LogHelper.w(
                    TAG,
                    "Skip refresh in lock: token missing access=${tokenSuffix(latestAccess)} refresh=${tokenSuffix(latestRefresh)} path=${requestPath(request)}"
                )
                return null
            }

            // 当前失败请求如果已经不是最新 token 了，只需替换请求头再重放，不必再次刷新。
            if (requestAuth != bearer(latestAccess)) {
                LogHelper.i(
                    TAG,
                    "Reuse latest access token for replay. latestAccess=${tokenSuffix(latestAccess)} path=${requestPath(request)}"
                )
                return request.newBuilder()
                    .header(AUTH_HEADER, bearer(latestAccess))
                    .build()
            }

            // 同一把 refresh token 在短时间窗口内刚失败过: 并发 401 风暴去抖。
            // 超过窗口后清空标记,允许网络抖动后的再次静默刷新。
            if (lastFailedRefreshToken == latestRefresh) {
                val elapsed = System.currentTimeMillis() - lastFailedRefreshAtMs
                if (elapsed < TRANSIENT_REFRESH_FAILURE_DEBOUNCE_MS) {
                    LogHelper.w(
                        TAG,
                        "Skip refresh: debounce short-circuit elapsed=${elapsed}ms refresh=${tokenSuffix(latestRefresh)} path=${requestPath(request)}"
                    )
                    return null
                }
                LogHelper.i(TAG, "Refresh debounce window elapsed, allowing retry. refresh=${tokenSuffix(latestRefresh)}")
                lastFailedRefreshToken = null
            }

            return when (val refreshed = refreshTokens(latestRefresh)) {
                is RefreshResult.Success -> {
                    lastFailedRefreshToken = null
                    lastFailedRefreshAtMs = 0L
                    // 刷新成功后立即同步会话状态，让后续请求都能读到最新 token。
                    sessionManager.updateTokens(refreshed.tokens.accessToken, refreshed.tokens.refreshToken)
                    LogHelper.i(
                        TAG,
                        "Refresh success. access=${tokenSuffix(refreshed.tokens.accessToken)} refresh=${tokenSuffix(refreshed.tokens.refreshToken)} path=${requestPath(request)}"
                    )
                    request.newBuilder()
                        .header(AUTH_HEADER, bearer(refreshed.tokens.accessToken))
                        .build()
                }

                RefreshResult.RefreshTokenInvalid -> {
                    lastFailedRefreshToken = latestRefresh
                    lastFailedRefreshAtMs = System.currentTimeMillis()
                    // refresh token 已失效时不要继续重试，直接让上层进入会话失效流程。
                    LogHelper.w(
                        TAG,
                        "Refresh token invalid. trigger RequireLogin refresh=${tokenSuffix(latestRefresh)} path=${requestPath(request)}"
                    )
                    sessionManager.refreshTokenInvalid()
                    null
                }

                RefreshResult.Failed -> {
                    lastFailedRefreshToken = latestRefresh
                    lastFailedRefreshAtMs = System.currentTimeMillis()
                    LogHelper.w(
                        TAG,
                        "Refresh request failed. notify AccessTokenRefreshFailed refresh=${tokenSuffix(latestRefresh)} path=${requestPath(request)}"
                    )
                    sessionManager.accessTokenRefreshFailed()
                    null
                }
            }
        }
    }

    private fun refreshTokens(refreshToken: String): RefreshResult {
        // 刷新接口固定挂在业务域名下，避免依赖外层 Retrofit 配置和拦截链导致循环调用。
        val url = "${config.baseUrl.trimEnd('/')}/api/auth/refresh"
        val payload = gson.toJson(RefreshRequestDto(refreshToken))
        val body = payload.toRequestBody(JSON)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return runCatching {
            refreshClient.newCall(request).execute().use { response ->
                // 服务端约定 403 表示 refresh token 不再可用，需要强制重新登录。
                if (response.code == 403) {
                    LogHelper.w(
                        TAG,
                        "Refresh API returns HTTP 403. refresh=${tokenSuffix(refreshToken)}"
                    )
                    return@use RefreshResult.RefreshTokenInvalid
                }
                if (!response.isSuccessful) {
                    LogHelper.w(
                        TAG,
                        "Refresh API failed with HTTP ${response.code}. refresh=${tokenSuffix(refreshToken)}"
                    )
                    return@use RefreshResult.Failed
                }
                val raw = response.body.string()
                val type = object : TypeToken<BaseResponse<AuthTokensDto>>() {}.type
                val envelope: BaseResponse<AuthTokensDto> = gson.fromJson(raw, type)
                if (envelope.code == 403) {
                    LogHelper.w(
                        TAG,
                        "Refresh API envelope code=403. refresh=${tokenSuffix(refreshToken)}"
                    )
                    return@use RefreshResult.RefreshTokenInvalid
                }
                if (!envelope.isSuccess()) {
                    LogHelper.w(
                        TAG,
                        "Refresh API envelope failed. code=${envelope.code} msg=${envelope.message}"
                    )
                    return@use RefreshResult.Failed
                }
                val tokens = envelope.data ?: run {
                    LogHelper.w(TAG, "Refresh API envelope missing token data.")
                    return@use RefreshResult.Failed
                }
                return@use RefreshResult.Success(tokens)
            }
        }.getOrElse { throwable ->
            LogHelper.e(TAG, "Refresh exception: ${throwable.javaClass.simpleName}", throwable)
            RefreshResult.Failed
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
    private fun tokenSuffix(token: String?): String = token?.takeLast(6)?.let { "***$it" } ?: "<null>"
    private fun requestPath(request: Request): String = request.url.encodedPath

    private companion object {
        private const val TAG = "Auth"
        private const val TRANSIENT_REFRESH_FAILURE_DEBOUNCE_MS = 2500
        // 鉴权相关请求统一复用同一个头名称，避免魔法字符串散落在重试逻辑里。
        const val AUTH_HEADER = "Authorization"
        // 刷新请求在这里手工拼 JSON 请求体，因此需要显式声明媒体类型。
        val JSON = "application/json; charset=utf-8".toMediaType()
    }

    private sealed class RefreshResult {
        data class Success(val tokens: AuthTokensDto) : RefreshResult()
        object RefreshTokenInvalid : RefreshResult()
        object Failed : RefreshResult()
    }
}
