package com.snapreceipt.io.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

/**
 * @property receiptId Receipt ID
 * @property merchant Merchant name
 * @property address Address
 * @property receiptDate Receipt date (yyyy-MM-dd)
 * @property receiptTime Receipt time (HH:mm:ss, optional)
 * @property totalAmount Total amount
 * @property tipAmount Tip amount
 * @property paymentCardNo Card number (last 4)
 * @property consumer Consumer name
 * @property remark Remark
 * @property receiptUrl Receipt image URL
 * @property categoryId Category ID
 * @property categoryName Category name
 * @property receiptType Receipt type (Business/Individual)
 */
@Parcelize
data class ReceiptEntity(
    val receiptId: Long? = null,
    val merchant: String? = null,
    val address: String? = null,
    val receiptDate: String? = null,
    val receiptTime: String? = null,
    val totalAmount: BigDecimal? = null,
    val tipAmount: BigDecimal? = null,
    val paymentCardNo: String? = null,
    val consumer: String? = null,
    val remark: String? = null,
    val receiptUrl: String? = null,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val receiptType: String? = null
) : Parcelable