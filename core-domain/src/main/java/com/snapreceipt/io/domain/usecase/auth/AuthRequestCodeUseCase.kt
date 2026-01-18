package com.snapreceipt.io.domain.usecase.auth

import com.snapreceipt.io.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRequestCodeUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(target: String): Result<Unit> =
        runCatching { repository.requestCode(target) }
}
