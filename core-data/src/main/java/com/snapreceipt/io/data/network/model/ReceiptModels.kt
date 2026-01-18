package com.snapreceipt.io.data.network.model

import com.google.gson.annotations.SerializedName
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.ReceiptScanResultEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScanRequestDto(
    @SerializedName("imageUrl") val imageUrl: String
)

data class ReceiptScanResultDto(
    @SerializedName("merchant") val merchant: String? = null,
    @SerializedName("receiptDate") val receiptDate: String? = null,
    @SerializedName("receiptTime") val receiptTime: String? = null,
    @SerializedName("totalAmount") val totalAmount: Double? = null,
    @SerializedName("tipAmount") val tipAmount: Double? = null,
    @SerializedName("paymentCardNo") val paymentCardNo: String? = null,
    @SerializedName("consumer") val consumer: String? = null,
    @SerializedName("remark") val remark: String? = null,
    @SerializedName("receiptUrl") val receiptUrl: String? = null
)

data class ReceiptSaveRequestDto(
    @SerializedName("merchant") val merchant: String,
    @SerializedName("receiptDate") val receiptDate: String,
    @SerializedName("totalAmount") val totalAmount: Double,
    @SerializedName("tipAmount") val tipAmount: Double,
    @SerializedName("paymentCardNo") val paymentCardNo: String,
    @SerializedName("consumer") val consumer: String,
    @SerializedName("remark") val remark: String? = null,
    @SerializedName("receiptUrl") val receiptUrl: String,
    @SerializedName("categoryId") val categoryId: Int,
    @SerializedName("receiptTime") val receiptTime: String? = null
)

data class ReceiptUpdateRequestDto(
    @SerializedName("receiptId") val receiptId: Long,
    @SerializedName("merchant") val merchant: String,
    @SerializedName("receiptDate") val receiptDate: String,
    @SerializedName("totalAmount") val totalAmount: Double,
    @SerializedName("tipAmount") val tipAmount: Double,
    @SerializedName("paymentCardNo") val paymentCardNo: String,
    @SerializedName("consumer") val consumer: String,
    @SerializedName("remark") val remark: String? = null,
    @SerializedName("receiptUrl") val receiptUrl: String? = null,
    @SerializedName("categoryId") val categoryId: Int,
    @SerializedName("receiptTime") val receiptTime: String? = null
)

data class ReceiptListRequestDto(
    @SerializedName("categoryId") val categoryId: Int? = null,
    @SerializedName("receiptDateStart") val receiptDateStart: String? = null,
    @SerializedName("receiptDateEnd") val receiptDateEnd: String? = null,
    @SerializedName("createTimeStart") val createTimeStart: String? = null,
    @SerializedName("createTimeEnd") val createTimeEnd: String? = null,
    @SerializedName("pageNum") val pageNum: Int? = null,
    @SerializedName("pageSize") val pageSize: Int? = null
)

data class ReceiptItemDto(
    @SerializedName("createBy") val createBy: String? = null,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("updateBy") val updateBy: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null,
    @SerializedName("remark") val remark: String? = null,
    @SerializedName("receiptId") val receiptId: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("categoryId") val categoryId: Int,
    @SerializedName("receiptType") val receiptType: String? = null,
    @SerializedName("receiptUrl") val receiptUrl: String,
    @SerializedName("merchant") val merchant: String,
    @SerializedName("totalAmount") val totalAmount: Double,
    @SerializedName("tipAmount") val tipAmount: Double? = null,
    @SerializedName("paymentCardNo") val paymentCardNo: String? = null,
    @SerializedName("consumer") val consumer: String? = null,
    @SerializedName("receiptDate") val receiptDate: String? = null,
    @SerializedName("receiptTime") val receiptTime: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("delFlag") val delFlag: String? = null
)

fun ReceiptItemDto.toEntity(): ReceiptEntity = ReceiptEntity(
    id = receiptId.toInt(),
    merchantName = merchant,
    amount = totalAmount,
    date = parseReceiptDateMillis(receiptDate, receiptTime),
    category = categoryId.toString(),
    invoiceType = receiptType ?: "normal",
    imagePath = receiptUrl,
    description = remark.orEmpty()
)

fun ReceiptEntity.toDto(): ReceiptItemDto = ReceiptItemDto(
    receiptId = id.toLong(),
    userId = 0L,
    categoryId = category.toIntOrNull() ?: 0,
    receiptType = invoiceType,
    receiptUrl = imagePath,
    merchant = merchantName,
    totalAmount = amount,
    receiptDate = formatDate(date, DATE_FORMAT),
    receiptTime = formatDate(date, TIME_FORMAT)
)

fun ReceiptScanResultDto.toEntity(): ReceiptScanResultEntity = ReceiptScanResultEntity(
    merchant = merchant,
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
