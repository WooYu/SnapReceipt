package com.snapreceipt.io.ui.home

/**
 * 首页自定义事件键。
 *
 * 用于 ViewModel 和 Fragment 之间传递“扫描预填就绪”“扫描失败”等一次性事件。
 */
object HomeEventKeys {
    // OCR 扫描成功后，通知页面打开详情页并带上预填数据。
    const val PREFILL_READY = "prefill_ready"
    // OCR 扫描失败后，通知页面弹出失败提示。
    const val SCAN_FAILED = "scan_failed"
    // 事件负载里携带的收据对象键名。
    const val EXTRA_ARGS = "extra_invoice_args"
}
