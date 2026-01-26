package com.snapreceipt.io.data.local.datasource

import com.snapreceipt.io.data.base.BaseLocalDataSource
import com.snapreceipt.io.data.db.ReceiptDao
import com.snapreceipt.io.data.db.ReceiptEntity
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReceiptLocalDataSource @Inject constructor(
    private val receiptDao: ReceiptDao,
    dispatchers: CoroutineDispatchersProvider
) : BaseLocalDataSource(dispatchers) {
    fun getAllReceipts(): Flow<List<ReceiptEntity>> = receiptDao.getAllReceipts()

    suspend fun getReceiptById(id: Long): ReceiptEntity? = withIo { receiptDao.getReceiptById(id) }

    suspend fun insertReceipt(receipt: ReceiptEntity): Long = withIo { receiptDao.insert(receipt) }

    suspend fun updateReceipt(receipt: ReceiptEntity) = withIo { receiptDao.update(receipt) }

    suspend fun deleteReceipt(receipt: ReceiptEntity) = withIo { receiptDao.delete(receipt) }

    suspend fun deleteReceipts(ids: List<Long>) = withIo { receiptDao.deleteMultiple(ids) }

    fun getReceiptsByDateRange(startDate: Long, endDate: Long): Flow<List<ReceiptEntity>> =
        receiptDao.getReceiptsByDateRange(startDate, endDate)

    fun getReceiptsByType(type: String): Flow<List<ReceiptEntity>> = receiptDao.getReceiptsByType(type)
}
