package com.skybound.space.core.network.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedAuthTokenStore(
    context: Context
) : AuthTokenStore {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun accessToken(): String? = prefs.getString(KEY_ACCESS, null)

    override fun refreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    override fun update(accessToken: String?, refreshToken: String?) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply()
    }

    override fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "auth_tokens_secure"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
