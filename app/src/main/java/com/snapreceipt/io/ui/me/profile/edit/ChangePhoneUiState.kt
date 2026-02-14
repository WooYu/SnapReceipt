package com.snapreceipt.io.ui.me.profile.edit

data class ChangePhoneUiState(
    val phone: String = "",
    val code: String = "",
    val loading: Boolean = false,
    val requestingCode: Boolean = false,
    val codeCountdownSeconds: Int = 0
)
