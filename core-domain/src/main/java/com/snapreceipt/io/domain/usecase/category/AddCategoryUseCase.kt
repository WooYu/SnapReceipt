package com.snapreceipt.io.domain.usecase.category

import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val repository: ReceiptRemoteRepository
) {
    suspend operator fun invoke(name: String): Result<Unit> =
        runCatching { repository.addCategory(name) }
}
