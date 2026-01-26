package com.snapreceipt.io.domain.model

/**
 * 发票领域实体（列表/详情展示使用）。
 *
 * 注意：后端使用 categoryId/receiptType 等字段，这里将分类映射为 label、日期时间合并为时间戳便于展示。
 *
 * @property id 发票ID（接口 receiptId）
 * @property merchantName 商户名称
 * @property amount 消费总额
 * @property currency 货币（接口暂未返回，保留展示字段）
 * @property date 发票时间戳（由 receiptDate + receiptTime 组合而来）
 * @property category 分类名称（由 categoryId 映射）
 * @property invoiceType 发票类型（Business/Individual）
 * @property imagePath 发票图片地址/路径
 * @property description 备注
 * @property createdAt 创建时间戳（本地生成）
 * @property updatedAt 更新时间戳（本地生成）
 */
data class ReceiptEntity(
    val id: Long = 0L,
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
