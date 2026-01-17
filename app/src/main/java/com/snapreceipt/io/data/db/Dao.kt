package com.snapreceipt.io.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Insert
    suspend fun insert(receipt: ReceiptEntity): Long

    @Update
    suspend fun update(receipt: ReceiptEntity)

    @Delete
    suspend fun delete(receipt: ReceiptEntity)

    @Query("SELECT * FROM receipts ORDER BY date DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptById(id: Int): ReceiptEntity?

    @Query("DELETE FROM receipts WHERE id IN (:ids)")
    suspend fun deleteMultiple(ids: List<Int>)

    @Query("SELECT * FROM receipts WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getReceiptsByDateRange(startDate: Long, endDate: Long): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE invoiceType = :type ORDER BY date DESC")
    fun getReceiptsByType(type: String): Flow<List<ReceiptEntity>>
}

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = 1")
    fun getUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = 1")
    suspend fun getUserSync(): UserEntity?
}
