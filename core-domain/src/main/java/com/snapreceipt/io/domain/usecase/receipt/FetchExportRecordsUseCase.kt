package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.query.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import javax.inject.Inject

class FetchExportRecordsUseCase @Inject constructor(
    private val repository: ReceiptRemoteRepository
) {
    suspend operator fun invoke(
        query: ExportRecordListQueryEntity = ExportRecordListQueryEntity()
    ): Result<List<ExportRecordEntity>> =
        runCatching { repository.listExportRecords(query) }
}
