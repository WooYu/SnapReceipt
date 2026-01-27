package com.snapreceipt.io.data.network.datasource

import com.snapreceipt.io.data.base.BaseRemoteDataSource
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.NetworkResult
import com.snapreceipt.io.data.network.model.auth.AuthCodeRequestDto
import com.snapreceipt.io.data.network.model.auth.AuthTokensDto
import com.snapreceipt.io.data.network.model.auth.LoginRequestDto
import com.snapreceipt.io.data.network.model.auth.RefreshRequestDto
import com.snapreceipt.io.data.network.model.auth.UserProfileDto
import com.snapreceipt.io.data.network.service.AuthApi

class AuthRemoteDataSource(
    private val api: AuthApi,
    dispatchers: CoroutineDispatchersProvider
) : BaseRemoteDataSource(dispatchers) {
    suspend fun requestCode(target: String): NetworkResult<Unit> {
        return requestUnit { api.requestCode(AuthCodeRequestDto(target)) }
    }

    suspend fun login(request: LoginRequestDto): NetworkResult<AuthTokensDto> {
        return request { api.login(request) }
    }

    suspend fun refresh(refreshToken: String): NetworkResult<AuthTokensDto> {
        return request { api.refresh(RefreshRequestDto(refreshToken)) }
    }

    suspend fun fetchUser(): NetworkResult<UserProfileDto> {
        return request { api.fetchUser() }
    }
}
