package com.snapreceipt.io.data.network.model.receipt

import com.snapreceipt.io.domain.model.ReceiptEntity

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
 * 将领域实体映射为保存请求 payload。
 * 这里显式控制字段，避免直接序列化 ReceiptEntity 带来无关字段上送。
 */
fun ReceiptEntity.toSaveRequestPayload(): Map<String, Any> = buildMap {
    put("merchant", merchant.orEmpty())
    put("receiptDate", receiptDate.orEmpty())
    put("totalAmount", totalAmount ?: 0.0)
    put("tipAmount", tipAmount ?: 0.0)
    put("paymentCardNo", paymentCardNo.orEmpty())
    put("consumer", consumer.orEmpty())
    put("receiptUrl", receiptUrl.orEmpty())
    put("categoryId", categoryId ?: 0L)
    putIfNotBlank("remark", remark)
    putIfNotBlank("receiptTime", receiptTime)
}

/**
 * 将领域实体映射为更新请求 payload。
 */
fun ReceiptEntity.toUpdateRequestPayload(): Map<String, Any> = buildMap {
    put("receiptId", receiptId ?: 0L)
    put("merchant", merchant.orEmpty())
    put("receiptDate", receiptDate.orEmpty())
    put("totalAmount", totalAmount ?: 0.0)
    put("tipAmount", tipAmount ?: 0.0)
    put("paymentCardNo", paymentCardNo.orEmpty())
    put("consumer", consumer.orEmpty())
    put("categoryId", categoryId ?: 0L)
    putIfNotBlank("remark", remark)
    putIfNotBlank("receiptTime", receiptTime)
    putIfNotBlank("receiptUrl", receiptUrl)
}

private fun MutableMap<String, Any>.putIfNotBlank(key: String, value: String?) {
    if (!value.isNullOrBlank()) {
        put(key, value)
    }
}
