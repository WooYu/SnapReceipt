package com.snapreceipt.io.ui.invoice

import android.content.Intent
import android.os.Build
import com.snapreceipt.io.domain.model.ReceiptEntity

/**
 * 简化版本：直接使用 Parcelable 传递数据，移除 JSON 序列化的复杂性
 */
object InvoiceDetailsArgsCodec {
    const val EXTRA_START_TAB = "extra_start_tab"
    const val EXTRA_RECEIPT = "extra_receipt"
    const val EXTRA_SOURCE_SCENE = "extra_source_scene"
    const val EXTRA_OPERATION_TYPE = "extra_operation_type"
    const val EXTRA_RECEIPT_ID = "extra_receipt_id"

    const val TAB_RECEIPTS = "receipts"

    const val SOURCE_SCAN = "scan_source"
    const val SOURCE_RECEIPTS_LIST = "receipts_list"
    const val SOURCE_INVOICES_LIST = "invoices_list"

    const val OPERATION_TYPE_ADD = "operation_add"
    const val OPERATION_TYPE_UPDATE = "operation_update"
    const val OPERATION_TYPE_DELETE = "operation_delete"

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
