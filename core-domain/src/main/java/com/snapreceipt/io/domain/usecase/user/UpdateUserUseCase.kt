package com.snapreceipt.io.domain.usecase.user

import com.snapreceipt.io.domain.model.UserEntity
import com.snapreceipt.io.domain.repository.UserRepository
import javax.inject.Inject

class UpdateUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(user: UserEntity): Result<Unit> =
        runCatching { repository.updateUser(user) }
}
