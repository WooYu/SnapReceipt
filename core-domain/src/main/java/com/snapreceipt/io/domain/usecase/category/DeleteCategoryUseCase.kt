package com.snapreceipt.io.domain.usecase.category

import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val repository: ReceiptRemoteRepository
) {
    suspend operator fun invoke(ids: List<Int>): Result<Unit> =
        runCatching { repository.removeCategories(ids) }
}
