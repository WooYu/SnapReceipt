package com.snapreceipt.io.domain.model

data class ReceiptSaveEntity(
    val merchant: String,
    val receiptDate: String,
    val totalAmount: Double,
    val tipAmount: Double,
    val paymentCardNo: String,
    val consumer: String,
    val remark: String? = null,
    val receiptUrl: String,
    val categoryId: Int,
    val receiptTime: String? = null
)
