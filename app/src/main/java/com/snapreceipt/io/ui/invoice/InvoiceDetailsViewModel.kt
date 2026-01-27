package com.snapreceipt.io.ui.invoice

import androidx.lifecycle.viewModelScope
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.usecase.receipt.DeleteReceiptRemoteUseCase
import com.snapreceipt.io.domain.usecase.receipt.SaveReceiptRemoteUseCase
import com.snapreceipt.io.domain.usecase.receipt.UpdateReceiptRemoteUseCase
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
    private val updateReceiptRemoteUseCase: UpdateReceiptRemoteUseCase,
    private val deleteReceiptRemoteUseCase: DeleteReceiptRemoteUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers, R.string.unexpected_error) {

    private val _uiState = MutableStateFlow(InvoiceDetailsUiState())
    val uiState: StateFlow<InvoiceDetailsUiState> = _uiState.asStateFlow()

    fun saveReceipt(receipt: ReceiptEntity) {
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

    fun updateReceipt(receipt: ReceiptEntity) {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch(dispatchers.io) {
            updateReceiptRemoteUseCase(receipt)
                .onSuccess {
                    _uiState.update { it.copy(loading = false) }
                    emitEvent(UiEvent.Custom(InvoiceDetailsEventKeys.SHOW_SUCCESS))
                    emitEvent(UiEvent.Custom(InvoiceDetailsEventKeys.NAVIGATE_TO_MAIN))
                }
                .onFailure { updateError(it) }
        }
    }

    fun deleteReceipt(receiptId: Long) {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch(dispatchers.io) {
            deleteReceiptRemoteUseCase(receiptId)
                .onSuccess {
                    _uiState.update { it.copy(loading = false) }
                    emitEvent(UiEvent.Custom(InvoiceDetailsEventKeys.SHOW_SUCCESS))
                    emitEvent(UiEvent.Custom(InvoiceDetailsEventKeys.NAVIGATE_TO_MAIN))
                }
                .onFailure { updateError(it) }
        }
    }

    private fun updateError(throwable: Throwable) {
        _uiState.update { it.copy(loading = false, error = throwable.message) }
        handleError(throwable)
    }
}
