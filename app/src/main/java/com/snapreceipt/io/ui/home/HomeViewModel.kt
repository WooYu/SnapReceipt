package com.snapreceipt.io.ui.home

import android.os.Bundle
import android.os.SystemClock
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
import com.skybound.space.core.monitoring.MonitoringNames
import com.skybound.space.core.monitoring.PerformanceMonitorManager
import com.skybound.space.core.monitoring.TrackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页收据列表 ViewModel。
 *
 * 负责协调三类场景：
 * 1. 收据列表的首屏加载、下拉刷新、分页加载；
 * 2. 本地即时增删改，保证从详情页返回后列表能立即响应；
 * 3. 拍照/相册上传后的 OCR 识别流程和埋点上报。
 */
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

    /**
     * 触发首页首屏或显式刷新加载。
     *
     * 固定从第一页开始，并展示页面级 loading。
     */
    fun loadReceipts() {
        fetchPage(page = 1, reset = true, showLoading = true)
    }

    /**
     * 下拉刷新入口。
     *
     * 与 [loadReceipts] 的区别是保留当前列表内容，仅显示刷新态。
     */
    fun refresh() {
        fetchPage(page = 1, reset = true, refreshing = true)
    }

    /**
     * 加载下一页数据。
     *
     * 只有在当前不处于任意加载态且服务端仍有更多数据时才会触发。
     */
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
            val receiptId = receipt.receiptId
            val deduped = if (receiptId != null) {
                current.receipts.filterNot { it.receiptId == receiptId }
            } else {
                current.receipts
            }
            val newList = listOf(receipt) + deduped
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

    /**
     * 分页拉取收据列表。
     *
     * @param page 目标页码
     * @param reset 是否重置现有列表；`true` 时用新数据覆盖旧列表，`false` 时追加到尾部
     * @param showLoading 是否展示页面主 loading
     * @param refreshing 是否展示下拉刷新态
     * @param loadingMore 是否展示底部分页加载态
     */
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

    /**
     * 删除指定收据并在成功后刷新列表。
     *
     * @param receipt 待删除收据；要求内部包含有效 receiptId
     */
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

    /**
     * 更新指定收据并在成功后刷新列表。
     *
     * @param receipt 待更新收据；必须带有 receiptId
     */
    fun updateReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch(dispatchers.io) {
            if (receipt.receiptId == null) return@launch
            updateReceiptRemoteUseCase(receipt)
                .onSuccess { loadReceipts() }
                .onFailure { updateError(it) }
        }
    }

    /**
     * 处理裁剪完成后的图片上传识别流程。
     *
     * 流程：
     * 1. 上报上传开始埋点；
     * 2. 调用 [UploadAndScanReceiptUseCase] 串行执行申请地址、上传、扫描；
     * 3. 成功时通知页面打开详情页，失败时触发扫描失败弹窗。
     *
     * @param imagePath 裁剪后图片本地路径
     */
    fun processCroppedImage(imagePath: String) {
        viewModelScope.launch(dispatchers.io) {
            val startedAt = SystemClock.elapsedRealtime()
            TrackManager.track(MonitoringNames.Events.receiptUploadStart)
            _uiState.update {
                it.copy(
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
                    PerformanceMonitorManager.trackApiCall(
                        endpoint = MonitoringNames.Traces.receiptUploadPipeline,
                        durationMs = SystemClock.elapsedRealtime() - startedAt
                    )
                    TrackManager.track(MonitoringNames.Events.receiptUploadSuccess)
                    _uiState.update {
                        it.copy(recognitionStatusResId = null)
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
                    PerformanceMonitorManager.trackApiCall(
                        endpoint = MonitoringNames.Traces.receiptUploadPipeline,
                        durationMs = SystemClock.elapsedRealtime() - startedAt
                    )
                    TrackManager.track(
                        MonitoringNames.Events.receiptUploadFail,
                        attributes = mapOf(
                            MonitoringNames.Params.reason to
                                (throwable.message ?: throwable.javaClass.simpleName).take(120)
                        )
                    )
                    _uiState.update {
                        it.copy(recognitionStatusResId = null)
                    }
                    emitEvent(UiEvent.Custom(HomeEventKeys.SCAN_FAILED))
                }
        }
    }

    /**
     * 统一收口列表加载相关的错误状态。
     *
     * @param throwable 失败异常；其 message 会用于页面错误文案展示
     */
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
