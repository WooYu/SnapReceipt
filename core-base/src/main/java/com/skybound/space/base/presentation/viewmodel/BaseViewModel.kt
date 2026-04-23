package com.skybound.space.base.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.UiState
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.ApiException
import com.skybound.space.core.util.LogHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseViewModel(
    private val dispatchers: CoroutineDispatchersProvider = CoroutineDispatchersProvider.Default,
    private val fallbackErrorResId: Int? = null
): ViewModel() {

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }

    protected fun emitEvent(event: UiEvent) {
        viewModelScope.launch(dispatchers.main) {
            _events.emit(event)
        }
    }

    /**
     * @param emptyAsEmpty When true, an empty [Collection] result is mapped to [UiState.Empty]
     *   instead of [UiState.Success]. Set to false when an empty list is a valid success state.
     */
    protected fun <T> launchOperation(
        state: MutableStateFlow<UiState<T>>,
        loadingState: UiState<T> = UiState.Loading(),
        emptyAsEmpty: Boolean = true,
        onError: (Throwable) -> UiState<T> = { throwable ->
            UiState.Error(
                message = throwable.message.orEmpty(),
                cause = throwable
            )
        },
        block: suspend CoroutineScope.() -> T
    ): Job = viewModelScope.launch(exceptionHandler) {
        withContext(dispatchers.main) { state.value = loadingState }
        val result = withContext(dispatchers.io) { runCatching { block() } }
        withContext(dispatchers.main) {
            result.onSuccess { data ->
                state.value = if (emptyAsEmpty && data is Collection<*> && data.isEmpty()) {
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
    }

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

    protected fun <T> launchWithLoading(
        updateLoading: (Boolean) -> Unit,
        block: suspend () -> Result<T>,
        onSuccess: (T) -> Unit,
        onFailure: (Throwable) -> Unit = { handleError(it) }
    ): Job = viewModelScope.launch(dispatchers.io + exceptionHandler) {
        withContext(dispatchers.main) { updateLoading(true) }
        block()
            .onSuccess { value ->
                withContext(dispatchers.main) {
                    updateLoading(false)
                    onSuccess(value)
                }
            }
            .onFailure { t ->
                withContext(dispatchers.main) {
                    updateLoading(false)
                    onFailure(t)
                }
            }
    }

    open fun handleError(throwable: Throwable) {
        if (throwable is CancellationException) {
            LogHelper.d("BaseViewModel", "Coroutine cancelled: ${throwable.message}")
            return
        }
        if (throwable is ApiException &&
            (throwable.code == ApiException.CODE_UNAUTHORIZED || throwable.code == ApiException.CODE_FORBIDDEN)
        ) {
            // SessionManager/Authenticator 会统一处理跳转,这里不再弹 toast
            return
        }
        emitEvent(
            UiEvent.Toast(
                message = throwable.message.orEmpty(),
                resId = fallbackErrorResId
            )
        )
    }

    protected fun <T> stateHolder(initial: UiState<T> = UiState.Idle): MutableStateFlow<UiState<T>> =
        MutableStateFlow(initial)
}