package com.snapreceipt.io.domain.model

data class ReceiptScanResultEntity(
    val merchant: String? = null,
    val address: String? = null,
    val receiptDate: String? = null,
    val receiptTime: String? = null,
    val totalAmount: Double? = null,
    val tipAmount: Double? = null,
    val paymentCardNo: String? = null,
    val consumer: String? = null,
    val remark: String? = null,
    val receiptUrl: String? = null
)
