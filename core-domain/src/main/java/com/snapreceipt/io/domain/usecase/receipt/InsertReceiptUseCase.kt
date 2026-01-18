package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.repository.ReceiptRepository
import javax.inject.Inject

class InsertReceiptUseCase @Inject constructor(
    private val repository: ReceiptRepository
) {
    suspend operator fun invoke(receipt: ReceiptEntity): Result<Long> =
        runCatching { repository.insertReceipt(receipt) }
}
