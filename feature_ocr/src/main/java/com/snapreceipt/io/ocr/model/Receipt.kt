package com.snapreceipt.io.ocr.model

data class Receipt(
    val id: String?,
    val title: String?,
    val amount: Double?,
    val date: String?,
    val type: String?
)
