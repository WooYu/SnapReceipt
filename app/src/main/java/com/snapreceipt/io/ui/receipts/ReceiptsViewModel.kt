package com.snapreceipt.io.ui.receipts

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.util.DateFormatUtil
import com.skybound.space.core.util.LogHelper
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.manager.CategoryCacheManager
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity
import com.snapreceipt.io.domain.usecase.receipt.DeleteReceiptRemoteUseCase
import com.snapreceipt.io.domain.usecase.receipt.ExportReceiptsRemoteUseCase
import com.snapreceipt.io.domain.usecase.receipt.FetchReceiptsUseCase
import com.snapreceipt.io.domain.usecase.receipt.UpdateReceiptRemoteUseCase
import com.snapreceipt.io.util.ReceiptTypeHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val fetchReceiptsUseCase: FetchReceiptsUseCase,
    private val deleteReceiptRemoteUseCase: DeleteReceiptRemoteUseCase,
    private val updateReceiptRemoteUseCase: UpdateReceiptRemoteUseCase,
    private val exportReceiptsRemoteUseCase: ExportReceiptsRemoteUseCase,
    
    private val receiptTypeHelper: ReceiptTypeHelper,
    private val categoryCache: CategoryCacheManager,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers, R.string.unexpected_error) {

    companion object {
        private const val LOG_TAG = "ReceiptsFilterVM"
    }

    private val _uiState = MutableStateFlow(ReceiptsUiState())
    val uiState: StateFlow<ReceiptsUiState> = _uiState.asStateFlow()
    private var titleTypeFilter: String? = null
    private var lastFetchedReceipts: List<ReceiptEntity> = emptyList()
    private var nextPage = 1
    private val pageSize = 20
    private var hasMore = true
    private var currentQuery = ReceiptListQueryEntity()

    init {
        loadReceipts()
    }

    fun loadReceipts() {
        currentQuery = ReceiptListQueryEntity()
        fetchPage(page = 1, reset = true, showLoading = true)
    }

    fun refresh() {
        fetchPage(page = 1, reset = true, refreshing = true)
    }

    fun loadMore() {
        if (!hasMore || _uiState.value.loadingMore || _uiState.value.loading || _uiState.value.refreshing) return
        fetchPage(page = nextPage, reset = false, loadingMore = true)
    }

    fun filterByDateRange(startDate: Long, endDate: Long) {
        val start = DateFormatUtil.formatApiDate(startDate)
        val end = DateFormatUtil.formatApiDate(endDate)
        LogHelper.d(LOG_TAG, "filterByDateRange start=$start end=$end")
        currentQuery = ReceiptListQueryEntity(
            receiptDateStart = start,
            receiptDateEnd = end
        )
        fetchPage(page = 1, reset = true, showLoading = true)
    }

    fun filterByInvoiceCategory(type: String) {
        val normalizedType = type.trim()
        val categoryId = categoryCache.idForLabel(normalizedType).takeIf { it > 0L }
        LogHelper.d(LOG_TAG, "filterByType label='$normalizedType' categoryId=$categoryId")
        currentQuery = ReceiptListQueryEntity(categoryId = categoryId)
        fetchPage(page = 1, reset = true, showLoading = true)
    }

    fun filterByReceiptType(type: String?) {
        val normalized = type?.trim().orEmpty()
        val payloadType = receiptTypeHelper.toPayloadValue(normalized)
        titleTypeFilter = payloadType.takeIf { it.isNotBlank() }
        LogHelper.d(
            LOG_TAG,
            "filterByTitleType input='$normalized' payload='${titleTypeFilter.orEmpty()}'"
        )
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
            var allSucceeded = true
            ids.forEach { id ->
                deleteReceiptRemoteUseCase(id)
                    .onFailure {
                        allSucceeded = false
                        updateError(it)
                    }
            }
            if (allSucceeded) {
                _uiState.update { it.copy(selectedIds = emptySet()) }
                fetchPage(page = 1, reset = true, showLoading = true)
            }
        }
    }

    fun deleteReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch(dispatchers.io) {
            val id = receipt.receiptId ?: return@launch
            deleteReceiptRemoteUseCase(id)
                .onSuccess { fetchPage(page = 1, reset = true, showLoading = true) }
                .onFailure { updateError(it) }
        }
    }

    fun updateReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch(dispatchers.io) {
            if (receipt.receiptId == null) return@launch
            updateReceiptRemoteUseCase(receipt)
                .onSuccess { fetchPage(page = 1, reset = true, showLoading = true) }
                .onFailure { updateError(it) }
        }
    }

    private fun fetchPage(
        page: Int,
        reset: Boolean,
        showLoading: Boolean = false,
        refreshing: Boolean = false,
        loadingMore: Boolean = false
    ) {
        _uiState.update {
            it.copy(
                loading = if (showLoading) true else it.loading,
                refreshing = if (refreshing) true else it.refreshing,
                loadingMore = if (loadingMore) true else it.loadingMore,
                error = null
            )
        }
        val query = applyDefaultDateFilter(currentQuery.copy(pageNum = page, pageSize = pageSize))
        LogHelper.d(
            LOG_TAG,
            "fetchPage page=$page reset=$reset showLoading=$showLoading refreshing=$refreshing loadingMore=$loadingMore query=$query titleTypeFilter='${titleTypeFilter.orEmpty()}'"
        )
        viewModelScope.launch(dispatchers.io) {
            fetchReceiptsUseCase(query)
                .onSuccess { receipts ->
                    val rawMerged = if (reset) receipts else lastFetchedReceipts + receipts
                    lastFetchedReceipts = rawMerged
                    hasMore = receipts.size >= pageSize
                    nextPage = if (hasMore) page + 1 else page
                    val filtered = applyTitleFilter(rawMerged)
                    LogHelper.d(
                        LOG_TAG,
                        "fetchPage success page=$page received=${receipts.size} merged=${rawMerged.size} filtered=${filtered.size} hasMore=$hasMore nextPage=$nextPage"
                    )
                    _uiState.update { current ->
                        val validIds = filtered.mapNotNull { it.receiptId }.toSet()
                        val nextSelected = current.selectedIds.intersect(validIds)
                        current.copy(
                            receipts = filtered,
                            selectedIds = nextSelected,
                            loading = false,
                            refreshing = false,
                            loadingMore = false,
                            error = null,
                            empty = filtered.isEmpty(),
                            hasLoaded = true,
                            hasMore = hasMore
                        )
                    }
                }
                .onFailure {
                    LogHelper.e(LOG_TAG, "fetchPage failed", it)
                    updateError(it)
                }
        }
    }

    private fun applyDefaultDateFilter(query: ReceiptListQueryEntity): ReceiptListQueryEntity {
        if (!query.receiptDateStart.isNullOrBlank() || !query.receiptDateEnd.isNullOrBlank()) {
            return query
        }
        return query.copy(receiptDateEnd = DateFormatUtil.todayApiDate())
    }

    private fun updateError(throwable: Throwable) {
        _uiState.update {
            it.copy(
                loading = false,
                refreshing = false,
                loadingMore = false,
                error = throwable.message,
                hasLoaded = true,
                hasMore = hasMore
            )
        }
        handleError(throwable)
    }

    private fun applyTitleFilter(receipts: List<ReceiptEntity> = _uiState.value.receipts): List<ReceiptEntity> {
        val payloadType = titleTypeFilter?.trim().orEmpty()
        if (payloadType.isBlank()) return receipts
        return receipts.filter { receipt ->
            val receiptPayload = receiptTypeHelper.toPayloadValue(receipt.receiptType.orEmpty())
            receiptPayload.equals(payloadType, ignoreCase = true)
        }
    }
}
