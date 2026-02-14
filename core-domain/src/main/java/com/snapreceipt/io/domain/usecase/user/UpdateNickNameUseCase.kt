package com.snapreceipt.io.domain.usecase.user

import com.snapreceipt.io.domain.repository.UserRepository
import javax.inject.Inject

class UpdateNickNameUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(nickName: String): Result<Unit> =
        runCatching { repository.updateNickName(nickName) }
}
