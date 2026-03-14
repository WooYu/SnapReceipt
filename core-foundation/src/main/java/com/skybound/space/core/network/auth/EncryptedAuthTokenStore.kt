package com.skybound.space.core.network.auth

import android.content.Context
import com.skybound.space.core.security.SecureStringPreferences

class EncryptedAuthTokenStore(
    context: Context
) : AuthTokenStore {

    private val prefs = SecureStringPreferences(context, PREFS_NAME)

    override fun accessToken(): String? = prefs.getString(KEY_ACCESS, null)

    override fun refreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    override fun update(accessToken: String?, refreshToken: String?) {
        prefs.putString(KEY_ACCESS, accessToken)
        prefs.putString(KEY_REFRESH, refreshToken)
    }

    override fun clear() {
        prefs.clear()
    }

    private companion object {
        const val PREFS_NAME = "auth_tokens_secure"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
