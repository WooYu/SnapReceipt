package com.snapreceipt.io.ui.invoice

import android.content.Intent
import android.os.Build
import com.snapreceipt.io.domain.model.ReceiptEntity

/**
 * 简化版本：直接使用 Parcelable 传递数据，移除 JSON 序列化的复杂性
 */
internal object InvoiceDetailsArgsCodec {
    private const val EXTRA_RECEIPT = "extra_receipt"

    fun writeReceipt(intent: Intent, receipt: ReceiptEntity) {
        intent.putExtra(EXTRA_RECEIPT, receipt)
    }

    fun readReceipt(intent: Intent): ReceiptEntity {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RECEIPT, ReceiptEntity::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RECEIPT)
        } ?: ReceiptEntity()
    }
}
