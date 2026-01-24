package com.skybound.space.core.network.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.skybound.space.core.util.LogHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    fun hasActiveSession(): Boolean =
        !tokenStore.accessToken().isNullOrBlank() && !tokenStore.refreshToken().isNullOrBlank()

    fun accessToken(): String? = tokenStore.accessToken()

    fun refreshToken(): String? = tokenStore.refreshToken()

    fun updateTokens(accessToken: String?, refreshToken: String?) {
        tokenStore.update(accessToken, refreshToken)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
        val accessSuffix = accessToken?.takeLast(6) ?: "null"
        val refreshSuffix = refreshToken?.takeLast(6) ?: "null"
        LogHelper.i(
            "Auth",
            "Token updated at $timestamp. access=...$accessSuffix refresh=...$refreshSuffix"
        )
    }

    fun refreshTokenInvalid() {
        tokenStore.clear()
        _events.tryEmit(SessionEvent.RequireLogin)
    }

    fun logout() {
        tokenStore.clear()
        _events.tryEmit(SessionEvent.LoggedOut)
    }
}
