package com.snapreceipt.io.ui.me.export

import androidx.lifecycle.viewModelScope
import com.snapreceipt.io.domain.model.ExportRecordListQueryEntity
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
) : BaseViewModel(dispatchers) {

    private val _uiState = MutableStateFlow(ExportRecordsUiState())
    val uiState: StateFlow<ExportRecordsUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    fun loadRecords(pageNum: Int = 1, pageSize: Int = 20) {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch(dispatchers.io) {
            fetchExportRecordsUseCase(ExportRecordListQueryEntity(pageNum, pageSize))
                .onSuccess { records ->
                    _uiState.update {
                        it.copy(
                            records = records,
                            loading = false,
                            error = null,
                            empty = records.isEmpty()
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
}
