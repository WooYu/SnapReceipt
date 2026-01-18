package com.snapreceipt.io.ui.receipts

import androidx.lifecycle.viewModelScope
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.usecase.receipt.DeleteReceiptUseCase
import com.snapreceipt.io.domain.usecase.receipt.DeleteReceiptsUseCase
import com.snapreceipt.io.domain.usecase.receipt.GetReceiptsByDateRangeUseCase
import com.snapreceipt.io.domain.usecase.receipt.GetReceiptsByTypeUseCase
import com.snapreceipt.io.domain.usecase.receipt.GetReceiptsUseCase
import com.snapreceipt.io.domain.usecase.receipt.UpdateReceiptUseCase
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptsViewModel @Inject constructor(
    private val getReceiptsUseCase: GetReceiptsUseCase,
    private val getReceiptsByDateRangeUseCase: GetReceiptsByDateRangeUseCase,
    private val getReceiptsByTypeUseCase: GetReceiptsByTypeUseCase,
    private val deleteReceiptsUseCase: DeleteReceiptsUseCase,
    private val deleteReceiptUseCase: DeleteReceiptUseCase,
    private val updateReceiptUseCase: UpdateReceiptUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers) {

    private val _uiState = MutableStateFlow(ReceiptsUiState())
    val uiState: StateFlow<ReceiptsUiState> = _uiState.asStateFlow()

    private var receiptsJob: Job? = null

    init {
        loadReceipts()
    }

    fun loadReceipts() {
        observeReceipts(getReceiptsUseCase())
    }

    fun filterByDateRange(startDate: Long, endDate: Long) {
        observeReceipts(getReceiptsByDateRangeUseCase(startDate, endDate))
    }

    fun filterByType(type: String) {
        observeReceipts(getReceiptsByTypeUseCase(type))
    }

    fun toggleSelection(id: Int) {
        _uiState.update { current ->
            val updated = current.selectedIds.toMutableSet()
            if (updated.contains(id)) {
                updated.remove(id)
            } else {
                updated.add(id)
            }
            current.copy(selectedIds = updated)
        }
    }

    fun selectAll() {
        val allIds = _uiState.value.receipts.map { it.id }.toSet()
        _uiState.update { it.copy(selectedIds = allIds) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(dispatchers.io) {
            deleteReceiptsUseCase(ids).onFailure { updateError(it) }
        }
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun deleteReceipt(receipt: ReceiptEntity) {
        runOperation { deleteReceiptUseCase(receipt) }
    }

    fun updateReceipt(receipt: ReceiptEntity) {
        runOperation { updateReceiptUseCase(receipt) }
    }

    private fun observeReceipts(flow: Flow<Result<List<ReceiptEntity>>>) {
        receiptsJob?.cancel()
        _uiState.update { it.copy(loading = true, error = null) }
        receiptsJob = viewModelScope.launch(dispatchers.io) {
            flow.collect { result ->
                result.onSuccess { receipts ->
                    _uiState.update { current ->
                        val validIds = receipts.map { it.id }.toSet()
                        val nextSelected = current.selectedIds.intersect(validIds)
                        current.copy(
                            receipts = receipts,
                            selectedIds = nextSelected,
                            loading = false,
                            error = null,
                            empty = receipts.isEmpty()
                        )
                    }
                }.onFailure { updateError(it) }
            }
        }
    }

    private fun runOperation(block: suspend () -> Result<*>) {
        viewModelScope.launch(dispatchers.io) {
            block().onFailure { updateError(it) }
        }
    }

    private fun updateError(throwable: Throwable) {
        _uiState.update { it.copy(loading = false, error = throwable.message ?: "Unexpected error") }
        handleError(throwable)
    }
}
