package com.snapreceipt.io.domain.model

/**
 * 扫码解析结果（对应 /api/receipt/scan 返回的 data）。
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
data class ReceiptScanResultEntity(
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
