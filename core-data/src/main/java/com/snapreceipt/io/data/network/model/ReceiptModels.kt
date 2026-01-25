package com.snapreceipt.io.data.network.model

import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.ReceiptListQueryEntity
import com.snapreceipt.io.domain.model.ReceiptSaveEntity
import com.snapreceipt.io.domain.model.ReceiptScanResultEntity
import com.snapreceipt.io.domain.model.ReceiptUpdateEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScanRequestDto(
    val imageUrl: String
)

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

data class CategoryListRequestDto(
    val unused: String? = null
)

data class CategoryCreateRequestDto(
    val categoryName: String
)

data class CategoryDeleteRequestDto(
    val categoryIds: List<Int>
)

data class CategoryItemDto(
    val categoryId: Int,
    val userId: Int? = null,
    val categoryName: String,
    val orderNum: Int? = null
)

data class ReceiptSaveRequestDto(
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
    val categoryId: Int,
    val receiptTime: String? = null
)

data class ReceiptDeleteRequestDto(
    val receiptId: Long
)

data class ReceiptExportRequestDto(
    val receiptIds: List<Long>
)

data class ReceiptListRequestDto(
    val categoryId: Int? = null,
    val receiptDateStart: String? = null,
    val receiptDateEnd: String? = null,
    val createTimeStart: String? = null,
    val createTimeEnd: String? = null,
    val pageNum: Int? = null,
    val pageSize: Int? = null
)

data class ExportRecordListRequestDto(
    val pageNum: Int? = null,
    val pageSize: Int? = null
)

data class ExportRecordItemDto(
    val createTime: String? = null,
    val exportId: Long,
    val beginDate: String? = null,
    val endDate: String? = null,
    val receiptCount: Int? = null,
    val exportType: String? = null,
    val totalAmount: Double? = null,
    val fileUrl: String? = null,
    val status: String? = null
)

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
    val delFlag: String? = null
)

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

fun ExportRecordItemDto.toEntity(): ExportRecordEntity = ExportRecordEntity(
    exportId = exportId,
    beginDate = beginDate.orEmpty(),
    endDate = endDate.orEmpty(),
    receiptCount = receiptCount ?: 0,
    exportType = exportType.orEmpty(),
    totalAmount = totalAmount ?: 0.0,
    fileUrl = fileUrl.orEmpty(),
    createTime = createTime,
    status = status
)

fun CategoryItemDto.toItem(): ReceiptCategory.Item = ReceiptCategory.Item(
    id = categoryId,
    label = categoryName,
    isCustom = (userId ?: 0) != 0
)

fun ReceiptEntity.toDto(): ReceiptItemDto = ReceiptItemDto(
    receiptId = id.toLong(),
    userId = 0L,
    categoryId = ReceiptCategory.idForLabel(category),
    receiptType = invoiceType,
    receiptUrl = imagePath,
    merchant = merchantName,
    totalAmount = amount,
    receiptDate = formatDate(date, DATE_FORMAT),
    receiptTime = formatDate(date, TIME_FORMAT)
)

fun ReceiptSaveEntity.toDto(): ReceiptSaveRequestDto = ReceiptSaveRequestDto(
    merchant = merchant,
    receiptDate = receiptDate,
    totalAmount = totalAmount,
    tipAmount = tipAmount,
    paymentCardNo = paymentCardNo,
    consumer = consumer,
    remark = remark,
    receiptUrl = receiptUrl,
    categoryId = categoryId,
    receiptTime = receiptTime
)

fun ReceiptUpdateEntity.toDto(): ReceiptUpdateRequestDto = ReceiptUpdateRequestDto(
    receiptId = receiptId,
    merchant = merchant,
    receiptDate = receiptDate,
    totalAmount = totalAmount,
    tipAmount = tipAmount,
    paymentCardNo = paymentCardNo,
    consumer = consumer,
    remark = remark,
    receiptUrl = receiptUrl,
    categoryId = categoryId,
    receiptTime = receiptTime
)

fun ReceiptListQueryEntity.toDto(): ReceiptListRequestDto = ReceiptListRequestDto(
    categoryId = categoryId,
    receiptDateStart = receiptDateStart,
    receiptDateEnd = receiptDateEnd,
    createTimeStart = createTimeStart,
    createTimeEnd = createTimeEnd,
    pageNum = pageNum,
    pageSize = pageSize
)

fun ExportRecordListQueryEntity.toDto(): ExportRecordListRequestDto = ExportRecordListRequestDto(
    pageNum = pageNum,
    pageSize = pageSize
)

fun ReceiptScanResultDto.toEntity(): ReceiptScanResultEntity = ReceiptScanResultEntity(
    merchant = merchant,
    address = address,
    receiptDate = receiptDate,
    receiptTime = receiptTime,
    totalAmount = totalAmount,
    tipAmount = tipAmount,
    paymentCardNo = paymentCardNo,
    consumer = consumer,
    remark = remark,
    receiptUrl = receiptUrl
)

fun ReceiptScanResultEntity.toDto(): ReceiptScanResultDto = ReceiptScanResultDto(
    merchant = merchant,
    address = address,
    receiptDate = receiptDate,
    receiptTime = receiptTime,
    totalAmount = totalAmount,
    tipAmount = tipAmount,
    paymentCardNo = paymentCardNo,
    consumer = consumer,
    remark = remark,
    receiptUrl = receiptUrl
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

private fun formatDate(timestamp: Long, pattern: String): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
