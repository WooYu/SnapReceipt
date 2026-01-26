package com.snapreceipt.io.domain.repository

import com.snapreceipt.io.domain.model.ReceiptEntity
import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {
    fun getAllReceipts(): Flow<List<ReceiptEntity>>
    suspend fun getReceiptById(id: Long): ReceiptEntity?
    suspend fun insertReceipt(receipt: ReceiptEntity): Long
    suspend fun updateReceipt(receipt: ReceiptEntity)
    suspend fun deleteReceipt(receipt: ReceiptEntity)
    suspend fun deleteReceipts(ids: List<Long>)
    fun getReceiptsByDateRange(startDate: Long, endDate: Long): Flow<List<ReceiptEntity>>
    fun getReceiptsByType(type: String): Flow<List<ReceiptEntity>>
}
