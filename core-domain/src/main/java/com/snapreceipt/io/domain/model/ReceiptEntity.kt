package com.snapreceipt.io.domain.model

/**
 * 发票领域实体（本地缓存/列表展示使用）。
 *
 * 注意：后端使用的是 categoryId/receiptType 等字段，本地实体使用 label 与时间戳做展示与缓存。
 *
 * @property id 本地数据库主键
 * @property merchantName 商户名称
 * @property amount 消费总额
 * @property currency 货币（本地展示用，接口暂未返回）
 * @property date 发票时间戳（由 receiptDate + receiptTime 组合而来）
 * @property category 分类名称（本地 label）
 * @property invoiceType 发票类型（Business/Individual）
 * @property imagePath 发票图片地址/路径
 * @property description 备注
 * @property createdAt 创建时间戳（本地）
 * @property updatedAt 更新时间戳（本地）
 */
data class ReceiptEntity(
    val id: Int = 0,
    val merchantName: String,
    val amount: Double,
    val currency: String = "CNY",
    val date: Long = System.currentTimeMillis(),
    val category: String = "",
    val invoiceType: String = "Individual",
    val imagePath: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
