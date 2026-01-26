package com.snapreceipt.io.domain.model

/**
 * 发票保存请求实体（对应 /api/receipt/save）。
 *
 * @property merchant 商户名称
 * @property receiptDate 发票日期（yyyy-MM-dd）
 * @property totalAmount 消费总额
 * @property tipAmount 小费金额
 * @property paymentCardNo 卡号（脱敏后的字符串）
 * @property consumer 消费者
 * @property remark 备注
 * @property receiptUrl 发票图片地址
 * @property categoryId 分类ID
 * @property receiptTime 发票时间（HH:mm:ss，可选）
 */
data class ReceiptSaveEntity(
    val merchant: String,
    val receiptDate: String,
    val totalAmount: Double,
    val tipAmount: Double,
    val paymentCardNo: String,
    val consumer: String,
    val remark: String? = null,
    val receiptUrl: String,
    val categoryId: Int,
    val receiptTime: String? = null
)
