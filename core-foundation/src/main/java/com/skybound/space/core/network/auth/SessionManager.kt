package com.skybound.space.core.network.auth

import kotlinx.coroutines.channels.BufferOverflow
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
    object AccessTokenRefreshFailed : SessionEvent()
}

@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: AuthTokenStore
) {
    // buffer>=4 + DROP_OLDEST: Activity 处于 STOPPED 时事件会在 buffer 里堆积,
    // 防止关键事件(如 RequireLogin)在紧随 AccessTokenRefreshFailed 时被 tryEmit 丢弃。
    private val _events = MutableSharedFlow<SessionEvent>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    // 同一次会话失效只发一次事件,防止并发 403/刷新失败路径重复清 token + 重复跳登录。
    // 成功登录或显式登出时重置。
    private val sessionInvalidated = AtomicBoolean(false)
    // Token 刷新失败的 Toast 只提示一次,避免 N 个并发失败请求各发一次 Toast 轰炸用户。
    // 成功刷新或重新登录后重置,允许下次失败再次提示。
    private val refreshFailNotified = AtomicBoolean(false)

    fun hasActiveSession(): Boolean =
        !tokenStore.accessToken().isNullOrBlank() && !tokenStore.refreshToken().isNullOrBlank()

    fun accessToken(): String? = tokenStore.accessToken()

    fun refreshToken(): String? = tokenStore.refreshToken()

    fun updateTokens(accessToken: String?, refreshToken: String?) {
        tokenStore.update(accessToken, refreshToken)
        if (!accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
            // 新一轮有效会话开始,允许下次真正失效时再次发事件/提示。
            sessionInvalidated.set(false)
            refreshFailNotified.set(false)
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
        // 会话彻底失效,吸收可能在途的 AccessTokenRefreshFailed,避免 Toast 与跳登录同屏出现。
        refreshFailNotified.set(true)
        tokenStore.clear()
        _events.tryEmit(SessionEvent.RequireLogin)
    }

    fun accessTokenRefreshFailed() {
        if (sessionInvalidated.get()) return
        if (!refreshFailNotified.compareAndSet(false, true)) return
        _events.tryEmit(SessionEvent.AccessTokenRefreshFailed)
    }

    fun logout() {
        // 无论之前是否已触发 RequireLogin,显式登出都认为进入未登录态。
        sessionInvalidated.set(true)
        tokenStore.clear()
        _events.tryEmit(SessionEvent.LoggedOut)
    }
}
