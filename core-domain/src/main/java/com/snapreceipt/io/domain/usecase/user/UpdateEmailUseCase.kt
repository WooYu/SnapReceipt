package com.snapreceipt.io.domain.usecase.user

import com.snapreceipt.io.domain.repository.UserRepository
import javax.inject.Inject

class UpdateEmailUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(email: String, code: String): Result<Unit> =
        runCatching { repository.updateEmail(email, code) }
}
