package com.snapreceipt.io.domain.usecase.user

import com.snapreceipt.io.domain.model.UserEntity
import com.snapreceipt.io.domain.repository.UserRepository
import com.snapreceipt.io.domain.result.asResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(): Flow<Result<UserEntity?>> = repository.getUser().asResult()
}
