package com.skybound.space.core.network.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.skybound.space.core.util.DateFormatUtil
import com.skybound.space.core.util.LogHelper
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

sealed class SessionEvent {
    object RequireLogin : SessionEvent()
    object LoggedOut : SessionEvent()
}

@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: AuthTokenStore
) {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    // 同一次会话失效只发一次事件,防止并发 403/刷新失败路径重复清 token + 重复跳登录。
    // 成功登录或显式登出时重置。
    private val sessionInvalidated = AtomicBoolean(false)

    fun hasActiveSession(): Boolean =
        !tokenStore.accessToken().isNullOrBlank() && !tokenStore.refreshToken().isNullOrBlank()

    fun accessToken(): String? = tokenStore.accessToken()

    fun refreshToken(): String? = tokenStore.refreshToken()

    fun updateTokens(accessToken: String?, refreshToken: String?) {
        tokenStore.update(accessToken, refreshToken)
        if (!accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
            // 新一轮有效会话开始,允许下次真正失效时再次发事件。
            sessionInvalidated.set(false)
        }
        val timestamp = DateFormatUtil.formatApiDateTime(System.currentTimeMillis())
        val accessSuffix = accessToken?.takeLast(6) ?: "null"
        val refreshSuffix = refreshToken?.takeLast(6) ?: "null"
        LogHelper.i(
            "Auth",
            "Token updated at $timestamp. access=...$accessSuffix refresh=...$refreshSuffix"
        )
    }

    fun refreshTokenInvalid() {
        if (!sessionInvalidated.compareAndSet(false, true)) {
            LogHelper.d("Auth", "refreshTokenInvalid ignored: already invalidated")
            return
        }
        tokenStore.clear()
        _events.tryEmit(SessionEvent.RequireLogin)
    }

    fun logout() {
        // 无论之前是否已触发 RequireLogin,显式登出都认为进入未登录态。
        sessionInvalidated.set(true)
        tokenStore.clear()
        _events.tryEmit(SessionEvent.LoggedOut)
    }
}
