package com.snapreceipt.io.ui.main

enum class MainTab {
    HOME,
    RECEIPTS,
    ME
}

data class MainUiState(
    val selectedTab: MainTab = MainTab.HOME,
    val loading: Boolean = false,
    val error: String? = null,
    val empty: Boolean = false
)
