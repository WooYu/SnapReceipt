package com.snapreceipt.io.ui.receipts

import com.snapreceipt.io.domain.model.ReceiptEntity

data class ReceiptsUiState(
    val receipts: List<ReceiptEntity> = emptyList(),
    val selectedIds: Set<Int> = emptySet(),
    val loading: Boolean = false,
    val exporting: Boolean = false,
    val error: String? = null,
    val empty: Boolean = true,
    val hasLoaded: Boolean = false
)
