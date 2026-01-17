package com.snapreceipt.io.data.network.model

import com.google.gson.annotations.SerializedName

data class ScanRequest(
    @SerializedName("imageUrl") val imageUrl: String
)

data class ReceiptScanResult(
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

data class ReceiptSaveRequest(
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

data class ReceiptUpdateRequest(
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

data class ReceiptListRequest(
    @SerializedName("categoryId") val categoryId: Int? = null,
    @SerializedName("receiptDateStart") val receiptDateStart: String? = null,
    @SerializedName("receiptDateEnd") val receiptDateEnd: String? = null,
    @SerializedName("createTimeStart") val createTimeStart: String? = null,
    @SerializedName("createTimeEnd") val createTimeEnd: String? = null,
    @SerializedName("pageNum") val pageNum: Int? = null,
    @SerializedName("pageSize") val pageSize: Int? = null
)

data class ReceiptItem(
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
