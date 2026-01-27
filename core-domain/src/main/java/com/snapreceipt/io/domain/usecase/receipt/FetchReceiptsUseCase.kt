package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import javax.inject.Inject

class FetchReceiptsUseCase @Inject constructor(
    private val repository: ReceiptRemoteRepository
) {
    suspend operator fun invoke(query: ReceiptListQueryEntity = ReceiptListQueryEntity()): Result<List<ReceiptEntity>> =
        runCatching { repository.list(query) }
}
