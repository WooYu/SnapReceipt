package com.snapreceipt.io.data.network.model.receipt

import com.snapreceipt.io.domain.model.ReceiptEntity

/**
 * 将领域实体映射为保存/更新请求 payload。
 * 仅在字段有值时上送，避免使用默认值污染请求体。
 */
fun ReceiptEntity.toRequestPayload(includeReceiptId: Boolean = false): Map<String, Any> = buildMap {
    if (includeReceiptId) {
        putIfNotNull("receiptId", receiptId)
    }
    putIfNotBlank("merchant", merchant)
    putIfNotBlank("address", address)
    putIfNotBlank("receiptDate", receiptDate)
    putIfNotNull("totalAmount", totalAmount)
    putIfNotNull("tipAmount", tipAmount)
    putIfNotBlank("paymentCardNo", paymentCardNo)
    putIfNotBlank("consumer", consumer)
    putIfNotBlank("receiptUrl", receiptUrl)
    putIfNotNull("categoryId", categoryId)
    putIfNotBlank("remark", remark)
    putIfNotBlank("receiptTime", receiptTime)
    putIfNotBlank("receiptType", receiptType)
}

private fun MutableMap<String, Any>.putIfNotBlank(key: String, value: String?) {
    if (!value.isNullOrBlank()) {
        put(key, value)
    }
}

private fun MutableMap<String, Any>.putIfNotNull(key: String, value: Any?) {
    if (value != null) {
        put(key, value)
    }
}
