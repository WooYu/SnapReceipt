package com.snapreceipt.io.ui.me.export

import androidx.lifecycle.viewModelScope
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.query.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.usecase.receipt.FetchExportRecordsUseCase
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
class ExportRecordsViewModel @Inject constructor(
    private val fetchExportRecordsUseCase: FetchExportRecordsUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers, R.string.unexpected_error) {

    private val _uiState = MutableStateFlow(ExportRecordsUiState())
    val uiState: StateFlow<ExportRecordsUiState> = _uiState.asStateFlow()
    private var nextPage = 1
    private val pageSize = 20
    private var hasMore = true

    init {
        loadRecords()
    }

    fun loadRecords() {
        fetchPage(page = 1, reset = true, showLoading = true)
    }

    fun refresh() {
        fetchPage(page = 1, reset = true, refreshing = true)
    }

    fun loadMore() {
        if (!hasMore || _uiState.value.loadingMore || _uiState.value.loading || _uiState.value.refreshing) return
        fetchPage(page = nextPage, reset = false, loadingMore = true)
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
        viewModelScope.launch(dispatchers.io) {
            fetchExportRecordsUseCase(ExportRecordListQueryEntity(page, pageSize))
                .onSuccess { records ->
                    val merged = if (reset) records else _uiState.value.records + records
                    hasMore = records.size >= pageSize
                    nextPage = if (hasMore) page + 1 else page
                    _uiState.update {
                        it.copy(
                            records = merged,
                            loading = false,
                            refreshing = false,
                            loadingMore = false,
                            error = null,
                            empty = merged.isEmpty(),
                            hasLoaded = true,
                            hasMore = hasMore
                        )
                    }
                }
                .onFailure { updateError(it) }
        }
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
}
