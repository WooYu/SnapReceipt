package com.skybound.space.base.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.UiState
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 轻量化的 ViewModel 基类，负责：
 * - 统一协程派发与异常处理
 * - UiEvent 一次性事件派发
 * - 为页面提供标准的状态流处理工具
 */
abstract class BaseViewModel(
    private val dispatchers: CoroutineDispatchersProvider = CoroutineDispatchersProvider.Default
): ViewModel() {

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }

    /**
     * 向 UI 发送一次性事件。
     */
    protected fun emitEvent(event: UiEvent) {
        viewModelScope.launch(dispatchers.main) {
            _events.emit(event)
        }
    }

    /**
     * 将通用的“加载 -> 成功/失败”流程标准化，减少各页面样板代码。
     */
    protected fun <T> launchOperation(
        state: MutableStateFlow<UiState<T>>,
        loadingState: UiState<T> = UiState.Loading(),
        onError: (Throwable) -> UiState<T> = { throwable ->
            UiState.Error(
                message = throwable.message ?: "Unexpected error",
                cause = throwable
            )
        },
        block: suspend CoroutineScope.() -> T
    ): Job = viewModelScope.launch(dispatchers.io + exceptionHandler) {
        state.value = loadingState
        val result = runCatching { block() }
        result.onSuccess { data ->
            state.value = if (data is Collection<*> && data.isEmpty()) {
                @Suppress("UNCHECKED_CAST")
                UiState.Empty as UiState<T>
            } else {
                UiState.Success(data)
            }
        }.onFailure { throwable ->
            state.value = onError(throwable)
            handleError(throwable)
        }
    }

    /**
     * 在 IO 线程执行任务并回调主线程。
     */
    protected fun <T> launchIo(
        onSuccess: suspend (T) -> Unit = {},
        onFailure: suspend (Throwable) -> Unit = { handleError(it) },
        block: suspend () -> T
    ): Job = viewModelScope.launch(dispatchers.io + exceptionHandler) {
        runCatching { block() }
            .onSuccess { value ->
                withContext(dispatchers.main) { onSuccess(value) }
            }.onFailure { throwable ->
                withContext(dispatchers.main) { onFailure(throwable) }
            }
    }

    /**
     * 子类可根据业务重写错误处理逻辑。
     */
    open fun handleError(throwable: Throwable) {
        emitEvent(
            UiEvent.Toast(
                message = throwable.message ?: "Unexpected error"
            )
        )
    }

    /**
     * 帮助方法：为简单页面快速创建状态流。
     */
    protected fun <T> stateHolder(initial: UiState<T> = UiState.Idle): MutableStateFlow<UiState<T>> =
        MutableStateFlow(initial)
}
