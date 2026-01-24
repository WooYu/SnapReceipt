package com.snapreceipt.io.ui.me.export

import com.snapreceipt.io.domain.model.ExportRecordEntity

data class ExportRecordsUiState(
    val records: List<ExportRecordEntity> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val empty: Boolean = true
)
