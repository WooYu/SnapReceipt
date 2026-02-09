package com.snapreceipt.io.data.network.model.receipt

/**
 * 扫码解析请求（/api/receipt/scan）。
 *
 * @property imageUrl 图片URL
 */
data class ScanRequestDto(
    val imageUrl: String
)

/**
 * 发票删除请求（/api/receipt/delete）。
 *
 * @property receiptId 发票ID
 */
data class ReceiptDeleteRequestDto(
    val receiptId: Long
)

/**
 * 发票导出请求（/api/receipt/export）。
 *
 * @property receiptIds 发票ID列表
 */
data class ReceiptExportRequestDto(
    val receiptIds: List<Long>
)
