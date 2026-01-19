package com.snapreceipt.io.ui.receipts

import androidx.lifecycle.viewModelScope
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.ReceiptListQueryEntity
import com.snapreceipt.io.domain.model.ReceiptUpdateEntity
import com.snapreceipt.io.domain.usecase.receipt.DeleteReceiptRemoteUseCase
import com.snapreceipt.io.domain.usecase.receipt.FetchReceiptsUseCase
import com.snapreceipt.io.domain.usecase.receipt.UpdateReceiptRemoteUseCase
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
class ReceiptsViewModel @Inject constructor(
    private val fetchReceiptsUseCase: FetchReceiptsUseCase,
    private val deleteReceiptRemoteUseCase: DeleteReceiptRemoteUseCase,
    private val updateReceiptRemoteUseCase: UpdateReceiptRemoteUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers) {

    private val _uiState = MutableStateFlow(ReceiptsUiState())
    val uiState: StateFlow<ReceiptsUiState> = _uiState.asStateFlow()

    init {
        loadReceipts()
    }

    fun loadReceipts() {
        fetchReceipts()
    }

    fun filterByDateRange(startDate: Long, endDate: Long) {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val query = ReceiptListQueryEntity(
            receiptDateStart = dateFormat.format(java.util.Date(startDate)),
            receiptDateEnd = dateFormat.format(java.util.Date(endDate))
        )
        fetchReceipts(query)
    }

    fun filterByType(type: String) {
        val categoryId = ReceiptCategory.idForLabel(type).takeIf { it > 0 }
        fetchReceipts(ReceiptListQueryEntity(categoryId = categoryId))
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
            ids.forEach { id ->
                deleteReceiptRemoteUseCase(id.toLong()).onFailure { updateError(it) }
            }
            fetchReceipts()
        }
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun deleteReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch(dispatchers.io) {
            deleteReceiptRemoteUseCase(receipt.id.toLong())
                .onSuccess { fetchReceipts() }
                .onFailure { updateError(it) }
        }
    }

    fun updateReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch(dispatchers.io) {
            updateReceiptRemoteUseCase(receipt.toUpdateEntity())
                .onSuccess { fetchReceipts() }
                .onFailure { updateError(it) }
        }
    }

    private fun fetchReceipts(query: ReceiptListQueryEntity = ReceiptListQueryEntity()) {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch(dispatchers.io) {
            fetchReceiptsUseCase(query)
                .onSuccess { receipts ->
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
                }
                .onFailure { updateError(it) }
        }
    }

    private fun updateError(throwable: Throwable) {
        _uiState.update { it.copy(loading = false, error = throwable.message ?: "Unexpected error") }
        handleError(throwable)
    }

    private fun ReceiptEntity.toUpdateEntity(): ReceiptUpdateEntity {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return ReceiptUpdateEntity(
            receiptId = id.toLong(),
            merchant = merchantName,
            receiptDate = dateFormat.format(java.util.Date(date)),
            receiptTime = timeFormat.format(java.util.Date(date)),
            totalAmount = amount,
            tipAmount = 0.0,
            paymentCardNo = "",
            consumer = "",
            remark = description,
            receiptUrl = imagePath,
            categoryId = ReceiptCategory.idForLabel(category)
        )
    }
}
