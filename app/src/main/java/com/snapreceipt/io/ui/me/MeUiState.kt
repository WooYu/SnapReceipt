package com.snapreceipt.io.ui.me

data class MeUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val empty: Boolean = false
)
