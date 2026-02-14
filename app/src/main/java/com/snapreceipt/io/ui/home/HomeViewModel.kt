package com.snapreceipt.io.ui.home

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity
import com.snapreceipt.io.domain.usecase.receipt.DeleteReceiptRemoteUseCase
import com.snapreceipt.io.domain.usecase.receipt.FetchReceiptsUseCase
import com.snapreceipt.io.domain.usecase.receipt.UpdateReceiptRemoteUseCase
import com.snapreceipt.io.domain.usecase.receipt.UploadAndScanReceiptUseCase
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fetchReceiptsUseCase: FetchReceiptsUseCase,
    private val updateReceiptRemoteUseCase: UpdateReceiptRemoteUseCase,
    private val deleteReceiptRemoteUseCase: DeleteReceiptRemoteUseCase,
    private val uploadAndScanReceiptUseCase: UploadAndScanReceiptUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers, R.string.unexpected_error) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var receiptsJob: Job? = null
    private var nextPage = 1
    private val pageSize = 20
    private var hasMore = true

    init {
        loadReceipts()
    }

    fun loadReceipts() {
        fetchPage(page = 1, reset = true, showLoading = true)
    }

    fun refresh() {
        fetchPage(page = 1, reset = true, refreshing = true)
    }

    fun loadMore() {
        if (!hasMore || _uiState.value.loadingMore || _uiState.value.loading || _uiState.value.refreshing) return
        fetchPage(page = nextPage, reset = false, loadingMore = true)
    }

    /**
     * 本地快速添加：新项直接插入到列表头部，无网络请求
     * 用于扫描成功场景，立即显示新增项
     * 分页逻辑：nextPage 和 hasMore 不变，下次加载更多时正确获取后续页面
     */
    fun addReceiptLocally(receipt: ReceiptEntity) {
        _uiState.update { current ->
            val newList = listOf(receipt) + current.receipts
            current.copy(
                receipts = newList,
                empty = false,
                hasLoaded = true
            )
        }
    }

    /**
     * 本地快速更新：找到对应项直接更新，无网络请求
     * 用于编辑保存场景，实时反映修改
     * 注意：返回后在后台补充刷新列表，确保服务端数据一致性
     */
    fun updateReceiptLocally(receipt: ReceiptEntity) {
        _uiState.update { current ->
            val updated = current.receipts.map {
                if (it.receiptId == receipt.receiptId) receipt else it
            }
            current.copy(receipts = updated)
        }
    }

    /**
     * 本地快速删除：直接移除列表中的项，无网络请求
     * 用于删除操作后，立即从列表消失
     * 分页逻辑：删除时不改变 nextPage/hasMore，但移除项数 < pageSize，
     *         下次加载更多会自动补齐，保证列表在线下拉时不出现空白
     */
    fun deleteReceiptLocally(receiptId: Long) {
        _uiState.update { current ->
            val updated = current.receipts.filter { it.receiptId != receiptId }
            current.copy(
                receipts = updated,
                empty = updated.isEmpty()
            )
        }
    }

    private fun fetchPage(
        page: Int,
        reset: Boolean,
        showLoading: Boolean = false,
        refreshing: Boolean = false,
        loadingMore: Boolean = false
    ) {
        receiptsJob?.cancel()
        _uiState.update {
            it.copy(
                loading = if (showLoading) true else it.loading,
                refreshing = if (refreshing) true else it.refreshing,
                loadingMore = if (loadingMore) true else it.loadingMore,
                error = null
            )
        }
        receiptsJob = viewModelScope.launch(dispatchers.io) {
            fetchReceiptsUseCase(ReceiptListQueryEntity(pageNum = page, pageSize = pageSize))
                .onSuccess { receipts ->
                    val merged = if (reset) receipts else _uiState.value.receipts + receipts
                    hasMore = receipts.size >= pageSize
                    nextPage = if (hasMore) page + 1 else page
                    _uiState.update { current ->
                        current.copy(
                            receipts = merged,
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

    fun deleteReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch(dispatchers.io) {
            val id = receipt.receiptId ?: return@launch
            deleteReceiptRemoteUseCase(id)
                .onSuccess { loadReceipts() }
                .onFailure { updateError(it) }
        }
    }

    fun insertReceipt(receipt: ReceiptEntity) {
        // No-op: remote save happens in InvoiceDetails.
    }

    fun updateReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch(dispatchers.io) {
            if (receipt.receiptId == null) return@launch
            updateReceiptRemoteUseCase(receipt)
                .onSuccess { loadReceipts() }
                .onFailure { updateError(it) }
        }
    }

    fun processCroppedImage(imagePath: String) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null,
                    recognitionStatusResId = R.string.uploading_receipt
                )
            }
            uploadAndScanReceiptUseCase(
                imagePath,
                onProgress = { stage ->
                    val resId = when (stage) {
                        UploadAndScanReceiptUseCase.Stage.REQUESTING_UPLOAD_URL,
                        UploadAndScanReceiptUseCase.Stage.UPLOADING -> R.string.uploading_receipt
                        UploadAndScanReceiptUseCase.Stage.SCANNING -> R.string.scanning_receipt
                    }
                    _uiState.update { it.copy(recognitionStatusResId = resId) }
                }
            )
                .onSuccess { scan ->
                    _uiState.update {
                        it.copy(loading = false, recognitionStatusResId = null)
                    }
                    emitEvent(
                        UiEvent.Custom(
                            HomeEventKeys.PREFILL_READY,
                            Bundle().apply {
                                putParcelable(
                                    HomeEventKeys.EXTRA_ARGS,
                                    scan
                                )
                            }
                        )
                    )
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = throwable.message,
                            recognitionStatusResId = null
                        )
                    }
                    emitEvent(UiEvent.Custom(HomeEventKeys.SCAN_FAILED))
                }
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
