package com.snapreceipt.io.ui.receipts

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity
import com.snapreceipt.io.domain.usecase.receipt.DeleteReceiptRemoteUseCase
import com.snapreceipt.io.domain.usecase.receipt.ExportReceiptsRemoteUseCase
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
    private val exportReceiptsRemoteUseCase: ExportReceiptsRemoteUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers, R.string.unexpected_error) {

    private val _uiState = MutableStateFlow(ReceiptsUiState())
    val uiState: StateFlow<ReceiptsUiState> = _uiState.asStateFlow()
    private var titleTypeFilter: String? = null
    private var lastFetchedReceipts: List<ReceiptEntity> = emptyList()

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
        val categoryId = ReceiptCategory.idForLabel(type).takeIf { it > 0L }
        fetchReceipts(ReceiptListQueryEntity(categoryId = categoryId))
    }

    fun filterByTitleType(type: String?) {
        titleTypeFilter = type?.trim().takeIf { !it.isNullOrBlank() }
        val filtered = applyTitleFilter(lastFetchedReceipts)
        _uiState.update { current ->
            val validIds = filtered.mapNotNull { it.receiptId }.toSet()
            current.copy(
                receipts = filtered,
                selectedIds = current.selectedIds.intersect(validIds),
                empty = filtered.isEmpty()
            )
        }
    }

    fun toggleSelection(id: Long) {
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
        val allIds = _uiState.value.receipts.mapNotNull { it.receiptId }.toSet()
        _uiState.update { it.copy(selectedIds = allIds) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun exportSelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty() || _uiState.value.exporting) return
        _uiState.update { it.copy(exporting = true) }
        viewModelScope.launch(dispatchers.io) {
            val result = exportReceiptsRemoteUseCase(ids)
            result.onSuccess {
                _uiState.update { it.copy(selectedIds = emptySet()) }
                val exportUrl = it.trim()
                if (exportUrl.isBlank()) {
                    emitEvent(
                        com.skybound.space.base.presentation.UiEvent.Toast(
                            message = "",
                            resId = R.string.export_file_unavailable
                        )
                    )
                } else {
                    val payload = Bundle().apply {
                        putString(ReceiptsEventKeys.EXPORT_URL, exportUrl)
                    }
                    emitEvent(
                        com.skybound.space.base.presentation.UiEvent.Custom(
                            ReceiptsEventKeys.SHOW_EXPORT_SUCCESS,
                            payload
                        )
                    )
                }
            }.onFailure { updateError(it) }
            _uiState.update { it.copy(exporting = false) }
        }
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(dispatchers.io) {
            ids.forEach { id ->
                deleteReceiptRemoteUseCase(id).onFailure { updateError(it) }
            }
            fetchReceipts()
        }
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun deleteReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch(dispatchers.io) {
            val id = receipt.receiptId ?: return@launch
            deleteReceiptRemoteUseCase(id)
                .onSuccess { fetchReceipts() }
                .onFailure { updateError(it) }
        }
    }

    fun updateReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch(dispatchers.io) {
            if (receipt.receiptId == null) return@launch
            updateReceiptRemoteUseCase(receipt)
                .onSuccess { fetchReceipts() }
                .onFailure { updateError(it) }
        }
    }

    private fun fetchReceipts(query: ReceiptListQueryEntity = ReceiptListQueryEntity()) {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch(dispatchers.io) {
            fetchReceiptsUseCase(applyDefaultDateFilter(query))
                .onSuccess { receipts ->
                    lastFetchedReceipts = receipts
                    val filtered = applyTitleFilter(lastFetchedReceipts)
                    _uiState.update { current ->
                        val validIds = filtered.mapNotNull { it.receiptId }.toSet()
                        val nextSelected = current.selectedIds.intersect(validIds)
                        current.copy(
                            receipts = filtered,
                            selectedIds = nextSelected,
                            loading = false,
                            error = null,
                            empty = filtered.isEmpty(),
                            hasLoaded = true
                        )
                    }
                }
                .onFailure { updateError(it) }
        }
    }

    private fun applyDefaultDateFilter(query: ReceiptListQueryEntity): ReceiptListQueryEntity {
        if (!query.receiptDateStart.isNullOrBlank() || !query.receiptDateEnd.isNullOrBlank()) {
            return query
        }
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = dateFormat.format(java.util.Date())
        return query.copy(receiptDateStart = today)
    }

    private fun updateError(throwable: Throwable) {
        _uiState.update { it.copy(loading = false, error = throwable.message) }
        handleError(throwable)
    }

    private fun applyTitleFilter(receipts: List<ReceiptEntity> = _uiState.value.receipts): List<ReceiptEntity> {
        val label = titleTypeFilter?.trim().orEmpty()
        if (label.isBlank()) return receipts
        return receipts.filter { it.receiptType?.equals(label, ignoreCase = true) == true }
    }
}
