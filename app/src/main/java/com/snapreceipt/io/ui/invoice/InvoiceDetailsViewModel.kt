package com.snapreceipt.io.ui.invoice

import androidx.lifecycle.viewModelScope
import com.snapreceipt.io.domain.model.ReceiptSaveEntity
import com.snapreceipt.io.domain.usecase.receipt.SaveReceiptRemoteUseCase
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceDetailsViewModel @Inject constructor(
    private val saveReceiptRemoteUseCase: SaveReceiptRemoteUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers) {

    private val _uiState = MutableStateFlow(InvoiceDetailsUiState())
    val uiState: StateFlow<InvoiceDetailsUiState> = _uiState.asStateFlow()

    fun saveReceipt(receipt: ReceiptSaveEntity) {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch(dispatchers.io) {
            saveReceiptRemoteUseCase(receipt)
                .onSuccess {
                    _uiState.update { it.copy(loading = false) }
                    emitEvent(UiEvent.Custom(InvoiceDetailsEventKeys.SHOW_SUCCESS))
                    emitEvent(UiEvent.Custom(InvoiceDetailsEventKeys.NAVIGATE_TO_MAIN))
                }
                .onFailure { updateError(it) }
        }
    }

    private fun updateError(throwable: Throwable) {
        _uiState.update { it.copy(loading = false, error = throwable.message ?: "Unexpected error") }
        handleError(throwable)
    }
}
