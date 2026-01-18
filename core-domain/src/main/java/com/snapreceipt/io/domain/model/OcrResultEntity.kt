package com.snapreceipt.io.domain.model

data class OcrResultEntity(
    val text: String,
    val merchant: String? = null,
    val amount: Double? = null
)
