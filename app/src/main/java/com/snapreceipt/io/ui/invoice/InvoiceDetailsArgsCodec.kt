package com.snapreceipt.io.ui.invoice

import android.content.Intent
import android.os.Build
import com.snapreceipt.io.domain.model.ReceiptEntity
import org.json.JSONObject

internal object InvoiceDetailsArgsCodec {
    const val EXTRA_ARGS = "extra_invoice_args"
    private const val EXTRA_RECEIPT_JSON = "extra_receipt_json"

    private object ReceiptJsonKeys {
        const val RECEIPT_ID = "receiptId"
        const val MERCHANT = "merchant"
        const val ADDRESS = "address"
        const val RECEIPT_DATE = "receiptDate"
        const val RECEIPT_TIME = "receiptTime"
        const val TOTAL_AMOUNT = "totalAmount"
        const val TIP_AMOUNT = "tipAmount"
        const val PAYMENT_CARD_NO = "paymentCardNo"
        const val CONSUMER = "consumer"
        const val REMARK = "remark"
        const val RECEIPT_URL = "receiptUrl"
        const val CATEGORY_ID = "categoryId"
        const val CATEGORY_NAME = "categoryName"
        const val RECEIPT_TYPE = "receiptType"
    }

    fun writeReceipt(intent: Intent, receipt: ReceiptEntity) {
        intent.putExtra(EXTRA_RECEIPT_JSON, receipt.toIntentJson())
    }

    fun readReceipt(intent: Intent): ReceiptEntity {
        val safeJson = intent.getStringExtra(EXTRA_RECEIPT_JSON).orEmpty()
        if (safeJson.isNotBlank()) {
            receiptFromIntentJson(safeJson)?.let { return it }
        }

        val legacyParcelable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ARGS, ReceiptEntity::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ARGS) as? ReceiptEntity
        }
        return legacyParcelable ?: ReceiptEntity()
    }

    private fun ReceiptEntity.toIntentJson(): String =
        JSONObject().apply {
            putIfNotNull(ReceiptJsonKeys.RECEIPT_ID, receiptId)
            putIfNotNull(ReceiptJsonKeys.MERCHANT, merchant)
            putIfNotNull(ReceiptJsonKeys.ADDRESS, address)
            putIfNotNull(ReceiptJsonKeys.RECEIPT_DATE, receiptDate)
            putIfNotNull(ReceiptJsonKeys.RECEIPT_TIME, receiptTime)
            putIfNotNull(ReceiptJsonKeys.TOTAL_AMOUNT, totalAmount)
            putIfNotNull(ReceiptJsonKeys.TIP_AMOUNT, tipAmount)
            putIfNotNull(ReceiptJsonKeys.PAYMENT_CARD_NO, paymentCardNo)
            putIfNotNull(ReceiptJsonKeys.CONSUMER, consumer)
            putIfNotNull(ReceiptJsonKeys.REMARK, remark)
            putIfNotNull(ReceiptJsonKeys.RECEIPT_URL, receiptUrl)
            putIfNotNull(ReceiptJsonKeys.CATEGORY_ID, categoryId)
            putIfNotNull(ReceiptJsonKeys.CATEGORY_NAME, categoryName)
            putIfNotNull(ReceiptJsonKeys.RECEIPT_TYPE, receiptType)
        }.toString()

    private fun receiptFromIntentJson(raw: String): ReceiptEntity? = runCatching {
        JSONObject(raw).toReceiptEntity()
    }.getOrNull()

    private fun JSONObject.toReceiptEntity(): ReceiptEntity = ReceiptEntity(
        receiptId = optNullable(ReceiptJsonKeys.RECEIPT_ID, JSONObject::getLong),
        merchant = optNullable(ReceiptJsonKeys.MERCHANT, JSONObject::getString),
        address = optNullable(ReceiptJsonKeys.ADDRESS, JSONObject::getString),
        receiptDate = optNullable(ReceiptJsonKeys.RECEIPT_DATE, JSONObject::getString),
        receiptTime = optNullable(ReceiptJsonKeys.RECEIPT_TIME, JSONObject::getString),
        totalAmount = optNullable(ReceiptJsonKeys.TOTAL_AMOUNT, JSONObject::getDouble),
        tipAmount = optNullable(ReceiptJsonKeys.TIP_AMOUNT, JSONObject::getDouble),
        paymentCardNo = optNullable(ReceiptJsonKeys.PAYMENT_CARD_NO, JSONObject::getString),
        consumer = optNullable(ReceiptJsonKeys.CONSUMER, JSONObject::getString),
        remark = optNullable(ReceiptJsonKeys.REMARK, JSONObject::getString),
        receiptUrl = optNullable(ReceiptJsonKeys.RECEIPT_URL, JSONObject::getString),
        categoryId = optNullable(ReceiptJsonKeys.CATEGORY_ID, JSONObject::getLong),
        categoryName = optNullable(ReceiptJsonKeys.CATEGORY_NAME, JSONObject::getString),
        receiptType = optNullable(ReceiptJsonKeys.RECEIPT_TYPE, JSONObject::getString)
    )

    private fun JSONObject.putIfNotNull(key: String, value: Any?) {
        if (value != null) put(key, value)
    }

    private inline fun <T> JSONObject.optNullable(
        key: String,
        getter: JSONObject.(String) -> T
    ): T? {
        if (!has(key) || isNull(key)) return null
        return getter(key)
    }
}
