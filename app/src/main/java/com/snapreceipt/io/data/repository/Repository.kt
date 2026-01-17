package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.db.ReceiptDao
import com.snapreceipt.io.data.db.ReceiptEntity
import com.snapreceipt.io.data.db.UserDao
import com.snapreceipt.io.data.db.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReceiptRepository @Inject constructor(
    private val receiptDao: ReceiptDao
) {
    fun getAllReceipts(): Flow<List<ReceiptEntity>> = receiptDao.getAllReceipts()

    suspend fun getReceiptById(id: Int): ReceiptEntity? = receiptDao.getReceiptById(id)

    suspend fun insertReceipt(receipt: ReceiptEntity): Long = receiptDao.insert(receipt)

    suspend fun updateReceipt(receipt: ReceiptEntity) = receiptDao.update(receipt)

    suspend fun deleteReceipt(receipt: ReceiptEntity) = receiptDao.delete(receipt)

    suspend fun deleteMultiple(ids: List<Int>) = receiptDao.deleteMultiple(ids)

    fun getReceiptsByDateRange(startDate: Long, endDate: Long): Flow<List<ReceiptEntity>> =
        receiptDao.getReceiptsByDateRange(startDate, endDate)

    fun getReceiptsByType(type: String): Flow<List<ReceiptEntity>> =
        receiptDao.getReceiptsByType(type)
}

class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    fun getUser(): Flow<UserEntity?> = userDao.getUser()

    suspend fun getUserSync(): UserEntity? = userDao.getUserSync()

    suspend fun updateUser(user: UserEntity) = userDao.update(user)

    suspend fun insertUser(user: UserEntity) = userDao.insert(user)
}
