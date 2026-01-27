package com.snapreceipt.io.domain.repository

import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.query.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity

interface ReceiptRemoteRepository {
    suspend fun scan(imageUrl: String): ReceiptEntity
    suspend fun save(request: ReceiptEntity)
    suspend fun update(request: ReceiptEntity)
    suspend fun delete(receiptId: Long)
    suspend fun list(query: ReceiptListQueryEntity): List<ReceiptEntity>
    suspend fun export(receiptIds: List<Long>): String
    suspend fun listExportRecords(query: ExportRecordListQueryEntity): List<ExportRecordEntity>
    suspend fun listCategories(): List<ReceiptCategory.Item>
    suspend fun addCategory(name: String)
    suspend fun removeCategories(ids: List<Long>)
}
