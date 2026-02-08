package com.snapreceipt.io.ui.receipts

object ReceiptsRefreshSignal {
    private var pendingRefresh: Boolean = false

    @Synchronized
    fun requestRefresh() {
        pendingRefresh = true
    }

    @Synchronized
    fun consumeRefresh(): Boolean {
        if (!pendingRefresh) return false
        pendingRefresh = false
        return true
    }
}
