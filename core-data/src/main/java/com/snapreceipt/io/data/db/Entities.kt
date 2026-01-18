package com.snapreceipt.io.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val merchantName: String,
    val amount: Double,
    val currency: String = "CNY",
    val date: Long = System.currentTimeMillis(),
    val category: String = "",
    val invoiceType: String = "normal",
    val imagePath: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: Int = 1,
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val avatar: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
