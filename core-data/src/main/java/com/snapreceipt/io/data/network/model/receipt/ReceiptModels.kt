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

/**
 * 发票扫码识别结果（/api/receipt/scan 返回的 data）。
 *
 * @property merchant 商户名称
 * @property address 地址
 * @property receiptDate 发票日期（yyyy-MM-dd）
 * @property receiptTime 发票时间（HH:mm:ss）
 * @property totalAmount 消费总额
 * @property tipAmount 小费
 * @property paymentCardNo 卡号（脱敏后的字符串）
 * @property consumer 消费者
 * @property remark 备注
 * @property receiptUrl 发票图片地址
 */
data class ReceiptScanResultDto(
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
