package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.repository.ReceiptRepository
import com.snapreceipt.io.domain.result.asResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReceiptsUseCase @Inject constructor(
    private val repository: ReceiptRepository
) {
    operator fun invoke(): Flow<Result<List<ReceiptEntity>>> =
        repository.getAllReceipts().asResult()
}
