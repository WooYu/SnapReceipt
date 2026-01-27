package com.snapreceipt.io.data.network.model.receipt

import com.snapreceipt.io.domain.model.ReceiptEntity

/**
 * 将列表条目映射为本地展示实体。
 */
fun ReceiptItemDto.toEntity(): ReceiptEntity = ReceiptEntity(
    receiptId = receiptId,
    merchant = merchant,
    address = address,
    receiptDate = receiptDate,
    receiptTime = receiptTime,
    totalAmount = totalAmount,
    tipAmount = tipAmount,
    paymentCardNo = paymentCardNo,
    consumer = consumer,
    remark = remark,
    receiptUrl = receiptUrl,
    categoryId = categoryId,
    receiptType = receiptType
)

/**
 * 将扫码结果映射为本地实体。
 */
fun ReceiptScanResultDto.toEntity(): ReceiptEntity = ReceiptEntity(
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

/**
 * 将领域实体映射为保存请求。
 */
fun ReceiptEntity.toSaveRequestDto(): ReceiptSaveRequestDto = ReceiptSaveRequestDto(
    merchant = merchant.orEmpty(),
    receiptDate = receiptDate.orEmpty(),
    totalAmount = totalAmount ?: 0.0,
    tipAmount = tipAmount ?: 0.0,
    paymentCardNo = paymentCardNo.orEmpty(),
    consumer = consumer.orEmpty(),
    remark = remark?.takeIf { it.isNotBlank() },
    receiptUrl = receiptUrl.orEmpty(),
    categoryId = categoryId ?: 0L,
    receiptTime = receiptTime?.takeIf { it.isNotBlank() }
)

/**
 * 将领域实体映射为更新请求。
 */
fun ReceiptEntity.toUpdateRequestDto(): ReceiptUpdateRequestDto = ReceiptUpdateRequestDto(
    receiptId = receiptId ?: 0L,
    merchant = merchant.orEmpty(),
    receiptDate = receiptDate.orEmpty(),
    totalAmount = totalAmount ?: 0.0,
    tipAmount = tipAmount ?: 0.0,
    paymentCardNo = paymentCardNo.orEmpty(),
    consumer = consumer.orEmpty(),
    remark = remark?.takeIf { it.isNotBlank() },
    receiptUrl = receiptUrl?.takeIf { it.isNotBlank() },
    categoryId = categoryId ?: 0L,
    receiptTime = receiptTime?.takeIf { it.isNotBlank() }
)
