package com.snapreceipt.io.ui.login

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.PolicyEntity
import com.snapreceipt.io.domain.usecase.auth.AuthFetchUserProfileUseCase
import com.snapreceipt.io.domain.usecase.auth.AuthLoginUseCase
import com.snapreceipt.io.domain.usecase.auth.AuthRequestCodeUseCase
import com.snapreceipt.io.domain.usecase.config.FetchPolicyUseCase
import com.snapreceipt.io.domain.usecase.user.InsertUserUseCase
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val requestCodeUseCase: AuthRequestCodeUseCase,
    private val loginUseCase: AuthLoginUseCase,
    private val fetchUserProfileUseCase: AuthFetchUserProfileUseCase,
    private val insertUserUseCase: InsertUserUseCase,
    private val fetchPolicyUseCase: FetchPolicyUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) :
    BaseViewModel(dispatchers, R.string.unexpected_error) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    private var codeCountdownJob: Job? = null
    private var policyCache: PolicyEntity? = null
    private var policyPrefetchJob: Job? = null

    init {
        prefetchPolicy()
    }

    fun switchToPhone() {
        _uiState.update { it.copy(mode = LoginMode.PHONE) }
    }

    fun switchToEmail() {
        _uiState.update { it.copy(mode = LoginMode.EMAIL) }
    }

    fun setAgreementAccepted(accepted: Boolean) {
        _uiState.update { it.copy(agreementAccepted = accepted) }
    }

    fun requestCode(target: String) {
        if (_uiState.value.codeCountdownSeconds > 0) return
        if (target.isBlank()) {
            val resId = if (_uiState.value.mode == LoginMode.EMAIL) {
                R.string.email_empty
            } else {
                R.string.phone_empty
            }
            emitEvent(UiEvent.Toast(message = "", resId = resId))
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch(dispatchers.io) {
            requestCodeUseCase(target)
                .onSuccess {
                    _uiState.update { it.copy(loading = false) }
                    startCodeCountdown()
                    emitEvent(
                        UiEvent.Custom(
                            LoginEventKeys.CODE_SENT,
                            Bundle().apply {
                                putString(LoginEventKeys.EXTRA_TARGET, target)
                            }
                        )
                    )
                }
                .onFailure { updateError(it) }
        }
    }

    fun submitPhoneLogin(phone: String, code: String) {
        if (phone.isBlank() || code.isBlank()) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.input_required))
            return
        }
        login(phone, code)
    }

    fun submitEmailLogin(email: String, code: String) {
        if (email.isBlank() || code.isBlank()) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.input_required))
            return
        }
        login(email, code)
    }

    fun showToast(message: String) {
        emitEvent(UiEvent.Toast(message = message))
    }

    fun openUserAgreement() {
        openPolicy(PolicyTarget.USER_AGREEMENT)
    }

    fun openPrivacyPolicy() {
        openPolicy(PolicyTarget.PRIVACY_POLICY)
    }

    private fun login(target: String, code: String) {
        if (!_uiState.value.agreementAccepted) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.agreement_required))
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        val timezone = TimeZone.getDefault().id
        viewModelScope.launch(dispatchers.io) {
            loginUseCase(target, code, timezone)
                .onSuccess {
                    fetchUserProfileUseCase()
                        .onSuccess { user ->
                            insertUserUseCase(user).onFailure { updateError(it) }
                        }
                        .onFailure { updateError(it) }
                    _uiState.update { it.copy(loading = false) }
                    emitEvent(UiEvent.Custom(LoginEventKeys.NAVIGATE_MAIN))
                }
                .onFailure { updateError(it) }
        }
    }

    private fun openPolicy(target: PolicyTarget) {
        viewModelScope.launch(dispatchers.io) {
            val prefetch = policyPrefetchJob
            if (policyCache == null && prefetch?.isActive == true) {
                prefetch.join()
            }
            val policy = policyCache ?: run {
                emitEvent(UiEvent.Toast(message = "", resId = R.string.unexpected_error))
                return@launch
            }
            val url = when (target) {
                PolicyTarget.USER_AGREEMENT -> policy.userAgreement
                PolicyTarget.PRIVACY_POLICY -> policy.privacyPolicy
            }.trim()
            if (url.isBlank()) {
                emitEvent(UiEvent.Toast(message = "", resId = R.string.unexpected_error))
                return@launch
            }
            emitEvent(
                UiEvent.Custom(
                    LoginEventKeys.OPEN_POLICY,
                    Bundle().apply { putString(LoginEventKeys.EXTRA_URL, url) }
                )
            )
        }
    }

    private fun prefetchPolicy() {
        if (policyCache != null || policyPrefetchJob?.isActive == true) return
        policyPrefetchJob = viewModelScope.launch(dispatchers.io) {
            fetchPolicyUseCase()
                .onSuccess { policyCache = it }
        }
    }

    private fun startCodeCountdown() {
        codeCountdownJob?.cancel()
        codeCountdownJob = viewModelScope.launch(dispatchers.default) {
            for (second in 60 downTo 1) {
                _uiState.update { it.copy(codeCountdownSeconds = second) }
                delay(1000)
            }
            _uiState.update { it.copy(codeCountdownSeconds = 0) }
        }
    }

    private fun updateError(throwable: Throwable) {
        _uiState.update { it.copy(loading = false, error = throwable.message) }
        handleError(throwable)
    }

    private enum class PolicyTarget {
        USER_AGREEMENT,
        PRIVACY_POLICY
    }
}
