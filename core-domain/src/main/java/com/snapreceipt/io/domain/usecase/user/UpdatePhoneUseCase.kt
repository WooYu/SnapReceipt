package com.snapreceipt.io.domain.usecase.user

import com.snapreceipt.io.domain.repository.UserRepository
import javax.inject.Inject

class UpdatePhoneUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(phoneNumber: String, code: String): Result<Unit> =
        runCatching { repository.updatePhone(phoneNumber, code) }
}
