package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.mapper.AuthTokensDtoToDomainMapper
import com.snapreceipt.io.data.network.datasource.AuthRemoteDataSource
import com.snapreceipt.io.data.network.model.LoginRequestDto
import com.snapreceipt.io.data.network.model.toEntity
import com.snapreceipt.io.domain.model.AuthTokensEntity
import com.snapreceipt.io.domain.model.UserEntity
import com.snapreceipt.io.domain.repository.AuthRepository
import com.skybound.space.core.network.ApiException
import com.skybound.space.core.network.NetworkError
import com.skybound.space.core.network.NetworkResult
import com.skybound.space.core.network.auth.SessionManager
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sessionManager: SessionManager,
    private val mapper: AuthTokensDtoToDomainMapper
) : AuthRepository {
    override suspend fun requestCode(target: String) {
        when (val result = remoteDataSource.requestCode(target)) {
            is NetworkResult.Success -> Unit
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun login(target: String, code: String, timezone: String): AuthTokensEntity {
        val request = LoginRequestDto(to = target, code = code, timezone = timezone)
        return when (val result = remoteDataSource.login(request)) {
            is NetworkResult.Success -> {
                val tokens = mapper.map(result.data)
                sessionManager.updateTokens(tokens.accessToken, tokens.refreshToken)
                tokens
            }
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun fetchUserProfile(): UserEntity {
        return when (val result = remoteDataSource.fetchUser()) {
            is NetworkResult.Success -> result.data.toEntity()
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    private fun NetworkResult.Failure.toApiException(): ApiException {
        return when (val error = error) {
            is NetworkError.Http -> ApiException(error.code, error.message, error.throwable)
            is NetworkError.Network -> ApiException(-1, error.message, error.throwable)
            is NetworkError.Serialization -> ApiException(-2, error.message, error.throwable)
            is NetworkError.Unexpected -> ApiException(-3, error.message, error.throwable)
        }
    }
}
