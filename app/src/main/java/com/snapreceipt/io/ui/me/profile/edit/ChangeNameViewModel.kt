package com.snapreceipt.io.ui.me.profile.edit

import androidx.lifecycle.viewModelScope
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.usecase.user.UpdateNickNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeNameViewModel @Inject constructor(
    private val updateNickNameUseCase: UpdateNickNameUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers, R.string.unexpected_error) {

    private val _uiState = MutableStateFlow(ChangeNameUiState())
    val uiState: StateFlow<ChangeNameUiState> = _uiState.asStateFlow()

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun submit() {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) {
            emitEvent(UiEvent.Toast(message = "", resId = R.string.name_empty))
            return
        }
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch(dispatchers.io) {
            updateNickNameUseCase(name)
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
}
