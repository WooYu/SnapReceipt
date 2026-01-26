package com.snapreceipt.io.domain.model

/**
 * 发票列表查询条件（对应 /api/receipt/list 请求体）。
 *
 * @property categoryId 分类ID
 * @property categoryName 分类名称（可选，后端支持时使用）
 * @property receiptType 发票类型（Business/Individual）
 * @property receiptDateStart 发票开始日期（yyyy-MM-dd）
 * @property receiptDateEnd 发票结束日期（yyyy-MM-dd）
 * @property createTimeStart 上传开始时间（yyyy-MM-dd）
 * @property createTimeEnd 上传结束时间（yyyy-MM-dd）
 * @property pageNum 页码
 * @property pageSize 每页数量
 */
data class ReceiptListQueryEntity(
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val receiptType: String? = "Individual",
    val receiptDateStart: String? = null,
    val receiptDateEnd: String? = null,
    val createTimeStart: String? = null,
    val createTimeEnd: String? = null,
    val pageNum: Int? = 1,
    val pageSize: Int? = 20
)
