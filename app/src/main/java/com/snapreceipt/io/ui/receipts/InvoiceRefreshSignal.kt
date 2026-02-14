package com.snapreceipt.io.ui.receipts

/**
 * 发票/收据刷新信号
 * 支持多个列表源的刷新通知，用于处理返回列表时需要刷新数据的场景
 */
object InvoiceRefreshSignal {
    // 刷新来源标记
    const val SOURCE_RECEIPTS = "receipts"  // 首页列表
    const val SOURCE_INVOICES = "invoices"  // 发票列表

    // 待刷新的来源列表
    private val pendingRefreshSources = mutableSetOf<String>()

    @Synchronized
    fun requestRefresh(source: String = SOURCE_RECEIPTS) {
        pendingRefreshSources.add(source)
    }

    @Synchronized
    fun consumeRefresh(source: String): Boolean {
        return pendingRefreshSources.remove(source)
    }

    @Synchronized
    fun hasRefresh(source: String): Boolean {
        return source in pendingRefreshSources
    }

    @Synchronized
    fun clearAll() {
        pendingRefreshSources.clear()
    }
}
