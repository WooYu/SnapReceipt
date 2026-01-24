package com.snapreceipt.io.ui.me

data class MeUiState(
    val username: String = "",
    val email: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val empty: Boolean = false
)
