package com.snapreceipt.io.ui.me

import com.snapreceipt.io.R
import com.snapreceipt.io.domain.usecase.auth.AuthFetchUserProfileUseCase
import com.snapreceipt.io.domain.usecase.user.GetUserUseCase
import com.snapreceipt.io.domain.usecase.user.InsertUserUseCase
import androidx.lifecycle.viewModelScope
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatchersProvider,
    private val sessionManager: SessionManager,
    private val getUserUseCase: GetUserUseCase,
    private val fetchUserProfileUseCase: AuthFetchUserProfileUseCase,
    private val insertUserUseCase: InsertUserUseCase
) : BaseViewModel(dispatchers, R.string.unexpected_error) {

    private val _uiState = MutableStateFlow(MeUiState())
    val uiState: StateFlow<MeUiState> = _uiState.asStateFlow()

    init {
        observeUser()
        refreshUserProfile()
    }

    fun showToast(message: String) {
        emitEvent(UiEvent.Toast(message = message))
    }

    fun logout() {
        sessionManager.logout()
        emitEvent(UiEvent.Custom(MeEventKeys.NAVIGATE_LOGIN))
    }

    private fun observeUser() {
        viewModelScope.launch(dispatchers.io) {
            getUserUseCase().collect { result ->
                val user = result.getOrNull()
                _uiState.update { current ->
                    current.copy(
                        username = user?.username.orEmpty(),
                        email = user?.email.orEmpty()
                    )
                }
            }
        }
    }

    private fun refreshUserProfile() {
        viewModelScope.launch(dispatchers.io) {
            fetchUserProfileUseCase()
                .onSuccess { user -> insertUserUseCase(user) }
                .onFailure { updateError(it) }
        }
    }

    private fun updateError(throwable: Throwable) {
        _uiState.update { it.copy(error = throwable.message) }
        handleError(throwable)
    }
}
