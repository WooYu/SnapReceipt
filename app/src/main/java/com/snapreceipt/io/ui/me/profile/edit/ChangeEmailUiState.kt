package com.snapreceipt.io.ui.me.profile.edit

data class ChangeEmailUiState(
    val email: String = "",
    val code: String = "",
    val loading: Boolean = false,
    val requestingCode: Boolean = false,
    val codeCountdownSeconds: Int = 0
)
