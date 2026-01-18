package com.snapreceipt.io.ui.invoice

data class InvoiceDetailsUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val empty: Boolean = false
)
