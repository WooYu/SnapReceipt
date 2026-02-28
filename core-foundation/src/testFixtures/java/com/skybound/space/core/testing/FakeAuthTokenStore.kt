package com.skybound.space.core.testing

import com.skybound.space.core.network.auth.AuthTokenStore

/**
 * In-memory [AuthTokenStore] for unit tests.
 *
 * Allows inspecting and pre-setting tokens without encrypted storage.
 */
class FakeAuthTokenStore(
    private var access: String? = null,
    private var refresh: String? = null
) : AuthTokenStore {

    override fun accessToken(): String? = access
    override fun refreshToken(): String? = refresh

    override fun update(accessToken: String?, refreshToken: String?) {
        access = accessToken
        refresh = refreshToken
    }

    override fun clear() {
        access = null
        refresh = null
    }
}