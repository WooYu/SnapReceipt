package com.snapreceipt.io.domain.usecase.user

import com.snapreceipt.io.domain.model.UserEntity
import com.snapreceipt.io.domain.repository.UserRepository
import javax.inject.Inject

class GetUserSyncUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(): Result<UserEntity?> =
        runCatching { repository.getUserSync() }
}
