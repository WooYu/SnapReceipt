package com.snapreceipt.io.domain.model

data class ExportRecordEntity(
    val exportId: Long,
    val beginDate: String,
    val endDate: String,
    val receiptCount: Int,
    val exportType: String,
    val totalAmount: Double,
    val fileUrl: String,
    val createTime: String? = null,
    val status: String? = null
)
