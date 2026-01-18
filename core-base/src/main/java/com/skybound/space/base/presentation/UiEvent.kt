package com.skybound.space.base.presentation

import android.os.Bundle
import androidx.annotation.StringRes

/**
 * 一次性事件模型：导航、提示、对话框等，不会因为重建而重复消费。
 */
sealed interface UiEvent {
    data class Toast(
        val message: String,
        @StringRes val resId: Int? = null,
        val long: Boolean = false
    ) : UiEvent

    data class Snackbar(
        val message: String,
        val actionLabel: String? = null,
        val actionId: String? = null
    ) : UiEvent

    data class Dialog(
        val title: String? = null,
        val message: String,
        val positive: String = "OK",
        val negative: String? = null
    ) : UiEvent

    data class Navigation(
        val command: NavigationCommand
    ) : UiEvent

    data object NavigateBack : UiEvent

    /**
     * 预留给特殊事件，避免在 Base 层绑死 UI 行为。
     */
    data class Custom(
        val type: String,
        val payload: Bundle? = null
    ) : UiEvent
}

/**
 * 统一的导航命令，用于解耦具体导航实现（NavController/Compose/NavHostFragment）。
 */
data class NavigationCommand(
    val route: String,
    val popUpTo: String? = null,
    val inclusive: Boolean = false,
    val launchSingleTop: Boolean = true
)
