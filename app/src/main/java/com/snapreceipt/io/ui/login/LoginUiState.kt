package com.snapreceipt.io.ui.login

enum class LoginMode {
    PHONE,
    EMAIL
}

data class LoginUiState(
    val mode: LoginMode = LoginMode.PHONE,
    val loading: Boolean = false,
    val error: String? = null,
    val empty: Boolean = false,
    val codeCountdownSeconds: Int = 0,
    val agreementAccepted: Boolean = false
)
