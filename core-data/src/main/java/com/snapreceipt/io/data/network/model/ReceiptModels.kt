package com.snapreceipt.io.data.network.model

import com.google.gson.annotations.SerializedName
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptEntity
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 扫码解析请求（/api/receipt/scan）。
 *
 * @property imageUrl 图片URL
 */
data class ScanRequestDto(
    val imageUrl: String
)

/**
 * 分类列表请求（/api/category/list，无参数）。
 */
class CategoryListRequestDto

/**
 * 分类新增请求（/api/category/remove，后端实际路径以接口为准）。
 *
 * @property categoryName 分类名称
 */
data class CategoryCreateRequestDto(
    val categoryName: String
)

/**
 * 分类删除请求（/api/category/remove）。
 *
 * @property categoryIds 分类ID列表
 */
data class CategoryDeleteRequestDto(
    val categoryIds: List<Int>
)

/**
 * 分类条目（/api/category/list 返回的 data 列表项）。
 *
 * @property createBy 创建人
 * @property createTime 创建时间
 * @property updateBy 更新人
 * @property updateTime 更新时间
 * @property remark 备注
 * @property categoryId 分类ID
 * @property userId 用户ID（0 表示后台默认分类）
 * @property categoryName 分类名称
 * @property categoryIcon 分类图标（备用）
 * @property orderNum 排序
 * @property status 状态
 * @property delFlag 删除标记
 */
data class CategoryItemDto(
    val createBy: String? = null,
    val createTime: String? = null,
    val updateBy: String? = null,
    val updateTime: String? = null,
    val remark: String? = null,
    val categoryId: Int,
    val userId: Int? = null,
    val categoryName: String,
    val categoryIcon: String? = null,
    val orderNum: Int? = null,
    val status: String? = null,
    val delFlag: String? = null
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
    val categoryId: Int,
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

/**
 * 将列表条目映射为本地展示实体。
 */
fun ReceiptItemDto.toEntity(): ReceiptEntity = ReceiptEntity(
    id = receiptId.toInt(),
    merchantName = merchant,
    amount = totalAmount,
    date = parseReceiptDateMillis(receiptDate, receiptTime),
    category = ReceiptCategory.labelForId(categoryId),
    invoiceType = receiptType ?: "Individual",
    imagePath = receiptUrl,
    description = remark.orEmpty()
)

/**
 * 将分类条目映射为 UI 使用的分类项。
 */
fun CategoryItemDto.toItem(): ReceiptCategory.Item = ReceiptCategory.Item(
    id = categoryId,
    label = categoryName,
    isCustom = (userId ?: 0) != 0
)

private const val DATE_FORMAT = "yyyy-MM-dd"
private const val TIME_FORMAT = "HH:mm:ss"

private fun parseReceiptDateMillis(date: String?, time: String?): Long {
    val value = listOfNotNull(date, time).joinToString(" ").trim()
    if (value.isBlank()) return System.currentTimeMillis()
    val format = if (time.isNullOrBlank()) DATE_FORMAT else "$DATE_FORMAT $TIME_FORMAT"
    return runCatching {
        SimpleDateFormat(format, Locale.getDefault()).parse(value)?.time
    }.getOrNull() ?: System.currentTimeMillis()
}
