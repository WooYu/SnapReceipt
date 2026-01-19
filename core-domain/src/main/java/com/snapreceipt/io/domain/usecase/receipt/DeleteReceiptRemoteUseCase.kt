package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import javax.inject.Inject

class DeleteReceiptRemoteUseCase @Inject constructor(
    private val repository: ReceiptRemoteRepository
) {
    suspend operator fun invoke(receiptId: Long): Result<Unit> =
        runCatching { repository.delete(receiptId) }
}
