package com.snapreceipt.io.ui.home

import com.snapreceipt.io.domain.model.ReceiptEntity

data class HomeUiState(
    val receipts: List<ReceiptEntity> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val empty: Boolean = true,
    val hasLoaded: Boolean = false,
    val hasMore: Boolean = true,
    /** ResId for OCR recognition progress (uploading/scanning). Non-null only during processCroppedImage. */
    val recognitionStatusResId: Int? = null
)
