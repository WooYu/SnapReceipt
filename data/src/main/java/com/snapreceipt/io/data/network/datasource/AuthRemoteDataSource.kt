package com.snapreceipt.io.data.network.datasource

import com.skybound.space.core.dispatcher.DispatchersProvider
import com.skybound.space.core.network.NetworkResult
import com.skybound.space.core.network.safeApiCall
import com.skybound.space.core.network.safeApiCallBasic
import com.snapreceipt.io.data.network.model.AuthCodeRequest
import com.snapreceipt.io.data.network.model.AuthTokens
import com.snapreceipt.io.data.network.model.LoginRequest
import com.snapreceipt.io.data.network.model.RefreshRequest
import com.snapreceipt.io.data.network.model.UserProfile
import com.snapreceipt.io.data.network.service.AuthApi

class AuthRemoteDataSource(
    private val api: AuthApi,
    private val dispatchers: DispatchersProvider
) {
    suspend fun requestCode(target: String): NetworkResult<Unit> {
        return safeApiCallBasic(dispatchers) { api.requestCode(AuthCodeRequest(target)) }
    }

    suspend fun login(request: LoginRequest): NetworkResult<AuthTokens> {
        return safeApiCall(dispatchers) { api.login(request) }
    }

    suspend fun refresh(refreshToken: String): NetworkResult<AuthTokens> {
        return safeApiCall(dispatchers) { api.refresh(RefreshRequest(refreshToken)) }
    }

    suspend fun fetchUser(): NetworkResult<UserProfile> {
        return safeApiCall(dispatchers) { api.fetchUser() }
    }
}
