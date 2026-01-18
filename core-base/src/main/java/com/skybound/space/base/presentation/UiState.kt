package com.skybound.space.base.presentation

/**
 * UI 层统一状态模型，避免各页面重复定义 Loading/Empty/Error 等状态。
 */
sealed interface UiState<out T> {
    /**
     * 初始/空闲状态，用于尚未触发加载时。
     */
    data object Idle : UiState<Nothing>

    /**
     * 加载状态，支持区分是否为刷新的场景。
     */
    data class Loading(val isRefreshing: Boolean = false) : UiState<Nothing>

    /**
     * 成功状态，承载实际数据。
     */
    data class Success<T>(val data: T) : UiState<T>

    /**
     * 空数据状态。
     */
    data object Empty : UiState<Nothing>

    /**
     * 错误状态，包含用户可见的消息与可选原始异常。
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val recoverable: Boolean = true
    ) : UiState<Nothing>
}
