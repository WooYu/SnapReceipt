package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.repository.ReceiptRepository
import javax.inject.Inject

class DeleteReceiptsUseCase @Inject constructor(
    private val repository: ReceiptRepository
) {
    suspend operator fun invoke(ids: List<Int>): Result<Unit> =
        runCatching { repository.deleteReceipts(ids) }
}
