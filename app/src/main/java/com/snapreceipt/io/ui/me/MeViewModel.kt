package com.snapreceipt.io.ui.me

import com.snapreceipt.io.R
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchersProvider,
    private val sessionManager: SessionManager
) : BaseViewModel(dispatchers, R.string.unexpected_error) {

    private val _uiState = MutableStateFlow(MeUiState())
    val uiState: StateFlow<MeUiState> = _uiState.asStateFlow()

    fun showToast(message: String) {
        emitEvent(UiEvent.Toast(message = message))
    }

    fun logout() {
        sessionManager.logout()
        emitEvent(UiEvent.Custom(MeEventKeys.NAVIGATE_LOGIN))
    }
}
