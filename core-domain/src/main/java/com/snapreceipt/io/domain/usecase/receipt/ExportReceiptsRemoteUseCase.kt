package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import javax.inject.Inject

class ExportReceiptsRemoteUseCase @Inject constructor(
    private val repository: ReceiptRemoteRepository
) {
    suspend operator fun invoke(receiptIds: List<Long>): Result<String> =
        runCatching { repository.export(receiptIds) }
}
