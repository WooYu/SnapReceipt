package com.snapreceipt.io.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @property receiptId 发票ID（接口 receiptId）
 * @property merchant 商户名称
 * @property address 地址
 * @property receiptDate 发票日期（yyyy-MM-dd）
 * @property receiptTime 发票时间（HH:mm:ss，可选）
 * @property totalAmount 消费总额
 * @property tipAmount 小费
 * @property paymentCardNo 卡号（脱敏）
 * @property consumer 消费者
 * @property remark 备注
 * @property receiptUrl 发票图片地址
 * @property categoryId 分类ID
 * @property receiptType 发票类型（Business/Individual）
 */
@Parcelize
data class ReceiptEntity(
    val receiptId: Long? = null,
    val merchant: String? = null,
    val address: String? = null,
    val receiptDate: String? = null,
    val receiptTime: String? = null,
    val totalAmount: Double? = null,
    val tipAmount: Double? = null,
    val paymentCardNo: String? = null,
    val consumer: String? = null,
    val remark: String? = null,
    val receiptUrl: String? = null,
    val categoryId: Long? = null,
    val receiptType: String? = null
) : Parcelable
