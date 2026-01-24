package com.snapreceipt.io.domain.repository

import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptListQueryEntity
import com.snapreceipt.io.domain.model.ReceiptSaveEntity
import com.snapreceipt.io.domain.model.ReceiptScanResultEntity
import com.snapreceipt.io.domain.model.ReceiptUpdateEntity

interface ReceiptRemoteRepository {
    suspend fun scan(imageUrl: String): ReceiptScanResultEntity
    suspend fun save(request: ReceiptSaveEntity)
    suspend fun update(request: ReceiptUpdateEntity)
    suspend fun delete(receiptId: Long)
    suspend fun list(query: ReceiptListQueryEntity): List<ReceiptEntity>
    suspend fun export(receiptIds: List<Long>)
    suspend fun listExportRecords(query: ExportRecordListQueryEntity): List<ExportRecordEntity>
    suspend fun listCategories(): List<ReceiptCategory.Item>
    suspend fun addCategory(name: String)
    suspend fun removeCategories(ids: List<Int>)
}
