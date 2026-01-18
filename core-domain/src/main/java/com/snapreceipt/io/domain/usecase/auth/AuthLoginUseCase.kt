package com.snapreceipt.io.domain.usecase.auth

import com.snapreceipt.io.domain.model.AuthTokensEntity
import com.snapreceipt.io.domain.repository.AuthRepository
import javax.inject.Inject

class AuthLoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        target: String,
        code: String,
        timezone: String
    ): Result<AuthTokensEntity> =
        runCatching { repository.login(target, code, timezone) }
}
