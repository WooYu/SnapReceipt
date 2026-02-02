package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.mapper.AuthTokensDtoToDomainMapper
import com.snapreceipt.io.data.network.datasource.AuthRemoteDataSource
import com.snapreceipt.io.data.network.model.auth.LoginRequestDto
import com.snapreceipt.io.data.network.model.auth.toEntity
import com.snapreceipt.io.domain.model.AuthTokensEntity
import com.snapreceipt.io.domain.model.UserEntity
import com.snapreceipt.io.domain.repository.AuthRepository
import com.skybound.space.core.network.getOrThrow
import com.skybound.space.core.network.auth.SessionManager
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sessionManager: SessionManager,
    private val mapper: AuthTokensDtoToDomainMapper
) : AuthRepository {
    override suspend fun requestCode(target: String) {
        remoteDataSource.requestCode(target).getOrThrow()
    }

    override suspend fun login(target: String, code: String, timezone: String): AuthTokensEntity {
        val request = LoginRequestDto(to = target, code = code, timezone = timezone)
        val dto = remoteDataSource.login(request).getOrThrow()
        val tokens = mapper.map(dto)
        sessionManager.updateTokens(tokens.accessToken, tokens.refreshToken)
        return tokens
    }

    override suspend fun fetchUserProfile(): UserEntity =
        remoteDataSource.fetchUser().getOrThrow().toEntity()
}
