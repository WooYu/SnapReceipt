package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.repository.ReceiptRepository
import com.snapreceipt.io.domain.result.asResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReceiptsByDateRangeUseCase @Inject constructor(
    private val repository: ReceiptRepository
) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<Result<List<ReceiptEntity>>> =
        repository.getReceiptsByDateRange(startDate, endDate).asResult()
}
