package com.snapreceipt.io.domain.model

/**
 * 导出记录项（对应 /api/record/list 返回 rows）。
 *
 * @property exportId 导出记录ID
 * @property beginDate 开始日期
 * @property endDate 结束日期
 * @property receiptCount 发票数量
 * @property exportType 发票类型（Business/Individual）
 * @property totalAmount 总金额
 * @property fileUrl 导出文件地址/文件名
 * @property createTime 创建时间
 * @property status 状态
 */
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
