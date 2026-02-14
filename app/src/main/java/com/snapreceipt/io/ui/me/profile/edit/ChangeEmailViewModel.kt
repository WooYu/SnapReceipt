package com.snapreceipt.io.ui.me.profile.edit

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.usecase.user.RequestProfileUpdateCodeUseCase
import com.snapreceipt.io.domain.usecase.user.UpdateEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeEmailViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val requestProfileUpdateCodeUseCase: RequestProfileUpdateCodeUseCase,
    private val updateEmailUseCase: UpdateEmailUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers, R.string.unexpected_error) {

    private val _uiState = MutableStateFlow(ChangeEmailUiState())
    val uiState: StateFlow<ChangeEmailUiState> = _uiState.asStateFlow()

    private var codeCountdownJob: Job? = null

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun updateCode(value: String) {
        _uiState.update { it.copy(code = value) }
    }

    fun requestCode() {
        val state = _uiState.value
        val email = state.email.trim()
        if (state.loading || state.requestingCode || state.codeCountdownSeconds > 0) return
        if (email.isBlank()) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.email_empty))
            return
        }
        _uiState.update { it.copy(requestingCode = true) }
        viewModelScope.launch(dispatchers.io) {
            requestProfileUpdateCodeUseCase(email)
                .onSuccess {
                    _uiState.update { it.copy(requestingCode = false) }
                    emitEvent(UiEvent.Toast(appContext.getString(R.string.code_sent, email)))
                    startCodeCountdown()
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(requestingCode = false) }
                    handleError(throwable)
                }
        }
    }

    fun submit() {
        val state = _uiState.value
        val email = state.email.trim()
        val code = state.code.trim()
        if (email.isBlank()) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.email_empty))
            return
        }
        if (code.isBlank()) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.code_empty))
            return
        }
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch(dispatchers.io) {
            updateEmailUseCase(email, code)
                .onSuccess {
                    _uiState.update { it.copy(loading = false) }
                    emitEvent(UiEvent.Toast(message = "", resId = R.string.profile_update_success))
                    emitEvent(UiEvent.NavigateBack)
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(loading = false) }
                    handleError(throwable)
                }
        }
    }

    private fun startCodeCountdown() {
        codeCountdownJob?.cancel()
        codeCountdownJob = viewModelScope.launch(dispatchers.default) {
            for (seconds in 60 downTo 1) {
                _uiState.update { it.copy(codeCountdownSeconds = seconds) }
                delay(1000)
            }
            _uiState.update { it.copy(codeCountdownSeconds = 0) }
        }
    }
}
