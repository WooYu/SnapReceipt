package com.skybound.space.core.testing

import com.skybound.space.core.network.auth.SessionManager

/**
 * Creates a [SessionManager] backed by [FakeAuthTokenStore] for testing.
 */
fun fakeSessionManager(
    accessToken: String? = null,
    refreshToken: String? = null
): SessionManager {
    val store = FakeAuthTokenStore(accessToken, refreshToken)
    return SessionManager(store)
}