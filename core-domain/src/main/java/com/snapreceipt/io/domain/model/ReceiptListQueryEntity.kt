package com.snapreceipt.io.domain.model

data class ReceiptListQueryEntity(
    val categoryId: Int? = null,
    val receiptDateStart: String? = null,
    val receiptDateEnd: String? = null,
    val createTimeStart: String? = null,
    val createTimeEnd: String? = null,
    val pageNum: Int? = 1,
    val pageSize: Int? = 20
)
