package com.skybound.space.core.network.auth

class InMemoryAuthTokenStore : AuthTokenStore {
    @Volatile
    private var access: String? = null
    @Volatile
    private var refresh: String? = null

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
