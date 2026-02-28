package com.snapreceipt.io.ui.me.profile.edit

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.usecase.user.RequestProfileUpdateCodeUseCase
import com.snapreceipt.io.domain.usecase.user.UpdatePhoneUseCase
import com.snapreceipt.io.util.ContactInputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePhoneViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val requestProfileUpdateCodeUseCase: RequestProfileUpdateCodeUseCase,
    private val updatePhoneUseCase: UpdatePhoneUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers, R.string.unexpected_error) {

    private val _uiState = MutableStateFlow(ChangePhoneUiState())
    val uiState: StateFlow<ChangePhoneUiState> = _uiState.asStateFlow()

    private var codeCountdownJob: Job? = null

    fun updatePhone(value: String) {
        _uiState.update { it.copy(phone = value) }
    }

    fun updateCode(value: String) {
        _uiState.update { it.copy(code = value) }
    }

    fun requestCode() {
        val state = _uiState.value
        val phone = state.phone.trim()
        if (state.loading || state.requestingCode || state.codeCountdownSeconds > 0) return
        if (phone.isBlank()) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.phone_empty))
            return
        }
        if (!ContactInputValidator.isPhoneValid(phone)) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.phone_invalid))
            return
        }
        _uiState.update { it.copy(requestingCode = true) }
        viewModelScope.launch(dispatchers.io) {
            requestProfileUpdateCodeUseCase(phone)
                .onSuccess {
                    _uiState.update { it.copy(requestingCode = false) }
                    emitEvent(UiEvent.Toast(appContext.getString(R.string.code_sent, phone)))
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
        val phone = state.phone.trim()
        val code = state.code.trim()
        if (phone.isBlank()) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.phone_empty))
            return
        }
        if (!ContactInputValidator.isPhoneValid(phone)) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.phone_invalid))
            return
        }
        if (code.isBlank()) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.code_empty))
            return
        }
        if (!ContactInputValidator.isVerificationCodeValid(code)) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.code_invalid))
            return
        }
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch(dispatchers.io) {
            updatePhoneUseCase(phone, code)
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
