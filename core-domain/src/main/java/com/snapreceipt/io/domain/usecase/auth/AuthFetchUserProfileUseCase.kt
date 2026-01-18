package com.snapreceipt.io.domain.usecase.auth

import com.snapreceipt.io.domain.model.UserEntity
import com.snapreceipt.io.domain.repository.AuthRepository
import javax.inject.Inject

class AuthFetchUserProfileUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<UserEntity> =
        runCatching { repository.fetchUserProfile() }
}
