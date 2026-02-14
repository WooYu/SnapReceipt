package com.snapreceipt.io.domain.usecase.user

import com.snapreceipt.io.domain.repository.UserRepository
import javax.inject.Inject

class RequestProfileUpdateCodeUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(target: String): Result<Unit> =
        runCatching { repository.requestProfileUpdateCode(target) }
}
