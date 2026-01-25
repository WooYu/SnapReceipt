package com.snapreceipt.io.ui.home

import com.snapreceipt.io.domain.model.ReceiptEntity

data class HomeUiState(
    val receipts: List<ReceiptEntity> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val empty: Boolean = true,
    val hasLoaded: Boolean = false
)
