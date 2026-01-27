package com.snapreceipt.io.data.network.model.receipt

import com.google.gson.annotations.SerializedName

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
 * 发票保存请求（/api/receipt/save）。
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
data class ReceiptSaveRequestDto(
    val merchant: String,
    val receiptDate: String,
    val totalAmount: Double,
    val tipAmount: Double,
    val paymentCardNo: String,
    val consumer: String,
    val remark: String? = null,
    val receiptUrl: String,
    val categoryId: Long,
    val receiptTime: String? = null
)

/**
 * 发票更新请求（/api/receipt/update）。
 *
 * @property receiptId 发票ID
 * @property merchant 商户名称
 * @property receiptDate 发票日期（yyyy-MM-dd）
 * @property totalAmount 消费总额
 * @property tipAmount 小费金额
 * @property paymentCardNo 卡号（脱敏后的字符串）
 * @property consumer 消费者
 * @property remark 备注
 * @property receiptUrl 发票图片地址（可选）
 * @property categoryId 分类ID
 * @property receiptTime 发票时间（HH:mm:ss，可选）
 */
data class ReceiptUpdateRequestDto(
    val receiptId: Long,
    val merchant: String,
    val receiptDate: String,
    val totalAmount: Double,
    val tipAmount: Double,
    val paymentCardNo: String,
    val consumer: String,
    val remark: String? = null,
    val receiptUrl: String? = null,
    val categoryId: Long,
    val receiptTime: String? = null
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

/**
 * 发票列表条目（/api/receipt/list 返回 rows）。
 *
 * @property createBy 创建人
 * @property createTime 上传时间
 * @property updateBy 更新人
 * @property updateTime 更新时间
 * @property remark 备注
 * @property receiptId 发票ID
 * @property userId 用户ID
 * @property categoryId 分类ID
 * @property receiptType 发票类型（Business/Individual）
 * @property receiptUrl 发票图片地址
 * @property merchant 商户名称
 * @property totalAmount 消费总额
 * @property tipAmount 小费
 * @property paymentCardNo 卡号
 * @property consumer 消费者
 * @property receiptDate 发票日期
 * @property receiptTime 发票时间
 * @property status 状态
 * @property delFlag 删除标记
 * @property categoryName 分类名称
 * @property email 邮箱
 * @property phoneNumber 电话（脱敏）
 * @property address 地址
 */
data class ReceiptItemDto(
    val createBy: String? = null,
    val createTime: String? = null,
    val updateBy: String? = null,
    val updateTime: String? = null,
    val remark: String? = null,
    val receiptId: Long,
    val userId: Long,
    val categoryId: Long,
    val receiptType: String? = null,
    val receiptUrl: String,
    val merchant: String,
    val totalAmount: Double,
    val tipAmount: Double? = null,
    val paymentCardNo: String? = null,
    val consumer: String? = null,
    val receiptDate: String? = null,
    val receiptTime: String? = null,
    val status: String? = null,
    val delFlag: String? = null,
    val categoryName: String? = null,
    val email: String? = null,
    @SerializedName("phonenumber") val phoneNumber: String? = null,
    val address: String? = null
)
