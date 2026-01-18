package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.repository.ReceiptRepository
import javax.inject.Inject

class DeleteReceiptUseCase @Inject constructor(
    private val repository: ReceiptRepository
) {
    suspend operator fun invoke(receipt: ReceiptEntity): Result<Unit> =
        runCatching { repository.deleteReceipt(receipt) }
}
