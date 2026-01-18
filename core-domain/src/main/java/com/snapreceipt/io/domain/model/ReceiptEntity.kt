package com.snapreceipt.io.domain.model

data class ReceiptEntity(
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
