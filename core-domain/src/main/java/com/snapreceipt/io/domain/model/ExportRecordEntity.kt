package com.snapreceipt.io.domain.model

import java.math.BigDecimal

/**
 * @property exportId Export record ID
 * @property beginDate Start date
 * @property endDate End date
 * @property receiptCount Receipt count
 * @property exportType Receipt type (Business/Individual)
 * @property totalAmount Total amount
 * @property fileUrl Export file URL
 * @property createTime Create time
 * @property createBy Creator
 * @property updateBy Updater
 * @property updateTime Update time
 * @property remark Remark
 * @property userId User ID
 * @property status Status
 * @property delFlag Delete flag
 */
data class ExportRecordEntity(
    val exportId: Long,
    val beginDate: String,
    val endDate: String,
    val receiptCount: Int,
    val exportType: String,
    val totalAmount: BigDecimal,
    val fileUrl: String,
    val createTime: String? = null,
    val status: String? = null,
    val createBy: String? = null,
    val updateBy: String? = null,
    val updateTime: String? = null,
    val remark: String? = null,
    val userId: Long? = null,
    val delFlag: String? = null
)