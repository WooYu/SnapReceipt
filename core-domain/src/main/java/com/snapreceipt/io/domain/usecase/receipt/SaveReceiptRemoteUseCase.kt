package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.model.ReceiptSaveEntity
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import javax.inject.Inject

class SaveReceiptRemoteUseCase @Inject constructor(
    private val repository: ReceiptRemoteRepository
) {
    suspend operator fun invoke(request: ReceiptSaveEntity): Result<Unit> =
        runCatching { repository.save(request) }
}
