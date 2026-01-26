package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.repository.ReceiptRepository
import javax.inject.Inject

class GetReceiptByIdUseCase @Inject constructor(
    private val repository: ReceiptRepository
) {
    suspend operator fun invoke(id: Long): Result<ReceiptEntity?> =
        runCatching { repository.getReceiptById(id) }
}
