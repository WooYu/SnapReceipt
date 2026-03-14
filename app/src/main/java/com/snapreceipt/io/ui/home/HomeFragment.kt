package com.snapreceipt.io.ui.home

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.FragmentHomeBinding
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.ui.home.dialogs.ScanFailedDialog
import com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity
import com.snapreceipt.io.ui.invoice.InvoiceDetailsArgsCodec
import com.snapreceipt.io.ui.main.HomeRefreshEvent
import com.snapreceipt.io.ui.main.ListRefreshViewModel
import com.snapreceipt.io.ui.widget.CurvedGradientDrawable
import com.snapreceipt.io.ui.widget.statefullist.StatefulListLayout
import com.skybound.space.base.presentation.BaseFragment
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.observeState
import com.skybound.space.base.platform.permission.FragmentPermissionHelper
import com.skybound.space.base.platform.permission.PermissionManager
import com.skybound.space.base.platform.permission.Permissions
import com.skybound.space.core.monitoring.MonitoringNames
import com.skybound.space.core.monitoring.PerformanceMonitorManager
import com.skybound.space.core.monitoring.TrackManager
import com.skybound.space.core.util.LogHelper
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.coroutines.launch

/**
 * 首页收据列表页面。
 *
 * 主要职责：
 * 1. 渲染收据列表及分页状态；
 * 2. 处理拍照、相册选图、裁剪与 OCR 发起；
 * 3. 接收详情页返回结果并同步更新首页列表；
 * 4. 承担首页级埋点与首屏耗时记录。
 */
@AndroidEntryPoint
class HomeFragment : BaseFragment<HomeViewModel>(R.layout.fragment_home) {
    companion object {
        private const val LOG_TAG = "HomeListRefresh"
    }

    override val viewModel: HomeViewModel by viewModels()
    
    private val refreshViewModel: ListRefreshViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HomeReceiptAdapter
    private var pendingCameraUri: Uri? = null
    private var shouldScrollToTopOnNextRender = false
    private var pendingHomeRefreshAfterLoad = false
    private var pendingHomeRefreshReason: String? = null
    private var firstScreenStartedAtMs: Long = 0L
    private var hasTrackedFirstScreenLoad = false
    private var pendingCaptureSource: String? = null

    private lateinit var permissionHelper: FragmentPermissionHelper

    // 用于启动 InvoiceDetailsActivity 并处理返回结果
    // 采用 ActivityResultContract 替代回调，实现现代化的结果传递
    private val invoiceDetailsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            lifecycleScope.launch {
                handleInvoiceResult(
                    operationType = data.getStringExtra(InvoiceDetailsArgsCodec.EXTRA_OPERATION_TYPE),
                    receipt = IntentCompat.getParcelableExtra(
                        data,
                        InvoiceDetailsArgsCodec.EXTRA_RECEIPT,
                        ReceiptEntity::class.java
                    ),
                    receiptId = data.getLongExtra(InvoiceDetailsArgsCodec.EXTRA_RECEIPT_ID, -1L)
                )
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            startCrop(uri)
        } else if (!success) {
            pendingCaptureSource = null
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            startCrop(uri)
        } else {
            pendingCaptureSource = null
        }
    }

    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val output = UCrop.getOutput(result.data ?: return@registerForActivityResult)
            if (output != null) {
                handleCroppedImage(output)
            } else {
                pendingCaptureSource = null
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val error = UCrop.getError(result.data ?: return@registerForActivityResult)
            LogHelper.e("Crop", "Crop failed", error)
            trackCaptureEvent(
                eventName = MonitoringNames.Events.receiptCaptureFail,
                source = pendingCaptureSource,
                extraAttributes = mapOf(MonitoringNames.Params.reason to "crop_error")
            )
            pendingCaptureSource = null
            Toast.makeText(
                requireContext(),
                error?.localizedMessage ?: getString(R.string.image_crop_failed),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            pendingCaptureSource = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = FragmentPermissionHelper(this)
    }

    override fun onResume() {
        super.onResume()
        if (refreshViewModel.consumeHomeDirty()) {
            requestHomeRefreshOrDefer("on_resume_consume_home_dirty")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)
        setupHeaderBackground()
        setupAdapter()
        setupListeners()
        observeRefreshEvents()
        firstScreenStartedAtMs = SystemClock.elapsedRealtime()
        hasTrackedFirstScreenLoad = false
        observeState(viewModel.uiState) { renderState(it) }
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * 初始化头部渐变背景，保持首页顶部视觉风格统一。
     */
    private fun setupHeaderBackground() {
        val startColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        val endColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryGradientEnd)
        val curveHeight = resources.displayMetrics.density * 45f
        binding.headerBg.background = CurvedGradientDrawable(startColor, endColor, curveHeight)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 根据 [HomeUiState] 渲染页面。
     *
     * 这里刻意先提交列表数据，再更新 footer/loading 状态，
     * 以避免 RecyclerView 因 footer 状态变化产生意外滚动。
     *
     * @param state 当前首页 UI 状态
     */
    private fun renderState(state: HomeUiState) {
        tryConsumePendingHomeRefresh(state)

        // Submit data first (async), then update footer state in callback
        adapter.setReceipts(state.receipts) {
            val binding = _binding ?: return@setReceipts
            maybeTrackFirstScreenLoad(state)
            if (shouldScrollToTopOnNextRender && state.receipts.isNotEmpty()) {
                binding.statefulList.recyclerView.scrollToPosition(0)
                shouldScrollToTopOnNextRender = false
            }
            binding.statefulList.submit(buildListState(state))
        }

        // 仅 OCR 识别（上传/扫描）时显示模态 loading，列表加载由 StatefulListLayout 内置 loading 展示
        state.recognitionStatusResId?.let { resId ->
            showLoading(true, getString(resId))
        } ?: showLoading(false)

        // OCR 识别期间禁用扫描/上传按钮，防止重复操作
        val isRecognizing = state.recognitionStatusResId != null
        binding.cardScan.isEnabled = !isRecognizing
        binding.cardUpload.isEnabled = !isRecognizing
    }

    /**
     * 请求首页刷新；若当前正处于加载态，则先挂起，待本轮加载结束后再消费。
     *
     * @param trigger 触发刷新的来源标记，便于日志排查是哪个入口触发了刷新
     */
    private fun requestHomeRefreshOrDefer(trigger: String) {
        val currentState = viewModel.uiState.value
        LogHelper.d(
            LOG_TAG,
            "request trigger=$trigger loading=${currentState.loading} refreshing=${currentState.refreshing} hasLoaded=${currentState.hasLoaded}"
        )
        if (!currentState.loading && !currentState.refreshing) {
            pendingHomeRefreshAfterLoad = false
            pendingHomeRefreshReason = null
            if (currentState.hasLoaded) {
                LogHelper.d(LOG_TAG, "execute trigger=$trigger action=refresh")
                viewModel.refresh()
            } else {
                LogHelper.d(LOG_TAG, "execute trigger=$trigger action=load")
                viewModel.loadReceipts()
            }
        } else {
            pendingHomeRefreshAfterLoad = true
            pendingHomeRefreshReason = trigger
            LogHelper.d(LOG_TAG, "defer trigger=$trigger")
        }
    }

    /**
     * 尝试消费上一轮被延迟的刷新请求。
     *
     * @param state 当前页面状态；仅在不处于加载态时才会真正执行刷新/重载
     */
    private fun tryConsumePendingHomeRefresh(state: HomeUiState) {
        if (!pendingHomeRefreshAfterLoad) return
        if (state.loading || state.refreshing) return
        val deferredReason = pendingHomeRefreshReason ?: "unknown"
        pendingHomeRefreshAfterLoad = false
        pendingHomeRefreshReason = null
        if (state.hasLoaded) {
            LogHelper.d(LOG_TAG, "consume deferred=$deferredReason action=refresh")
            viewModel.refresh()
        } else {
            LogHelper.d(LOG_TAG, "consume deferred=$deferredReason action=load")
            viewModel.loadReceipts()
        }
    }

    /**
     * 初始化列表适配器和列表控件回调。
     */
    private fun setupAdapter() {
        adapter = HomeReceiptAdapter { receipt ->
            openReceiptForEdit(receipt)
        }
        binding.statefulList.setAdapter(adapter)
        binding.statefulList.setOnRefreshListener {
            LogHelper.d(LOG_TAG, "ui pull_to_refresh")
            viewModel.refresh()
        }
        binding.statefulList.setOnLoadMoreListener {
            LogHelper.d(LOG_TAG, "ui load_more")
            viewModel.loadMore()
        }
        binding.statefulList.setOnRetryListener {
            LogHelper.d(LOG_TAG, "ui retry_load")
            viewModel.loadReceipts()
        }
    }

    /**
     * 绑定拍照和相册上传入口。
     */
    private fun setupListeners() {
        binding.cardScan.setOnClickListener { openCameraWithPermission() }
        binding.cardUpload.setOnClickListener { pickImageFromGallery() }
    }

    /**
     * 监听来自详情页/其他页面的首页刷新事件。
     *
     * 这里优先做本地即时更新，只有无法准确增量更新时才退回整页刷新。
     */
    private fun observeRefreshEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                refreshViewModel.refreshHomeEvent.collect { event ->
                    when (event) {
                        is HomeRefreshEvent.ItemAdded -> {
                            LogHelper.d(LOG_TAG, "event item_added -> local insert")
                            viewModel.addReceiptLocally(event.receipt)
                        }
                        is HomeRefreshEvent.ItemUpdated -> {
                            LogHelper.d(LOG_TAG, "event item_updated -> local update")
                            viewModel.updateReceiptLocally(event.receipt)
                        }
                        is HomeRefreshEvent.ItemDeleted -> {
                            LogHelper.d(LOG_TAG, "event item_deleted -> local delete")
                            viewModel.deleteReceiptLocally(event.receiptId)
                        }
                        is HomeRefreshEvent.FullRefresh -> {
                            refreshViewModel.consumeHomeDirty()
                            requestHomeRefreshOrDefer("home_refresh_event_full_refresh")
                        }
                    }
                }
            }
        }
    }

    /**
     * 打开已保存收据详情页进行编辑。
     *
     * @param receipt 当前列表中被点击的收据
     */
    private fun openReceiptForEdit(receipt: ReceiptEntity) {
        // 从列表点击打开已保存的收据进行编辑
        // 标记来源为 SOURCE_RECEIPTS_LIST，用于判断初始状态（预览或编辑）
        val intent = InvoiceDetailsActivity.createIntent(
            requireContext(),
            receipt,
            InvoiceDetailsArgsCodec.SOURCE_RECEIPTS_LIST
        )
        invoiceDetailsLauncher.launch(intent)
    }

    /**
     * 打开相机前先检查相机权限。
     */
    private fun openCameraWithPermission() {
        if (!PermissionManager.needsPermission(requireContext(), Permissions.CAMERA)) {
            openCamera()
            return
        }
        permissionHelper.requestPermission(
            Permissions.CAMERA,
            onGranted = { openCamera() },
            onDenied = {
                Toast.makeText(requireContext(), getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
            }
        )
    }

    /**
     * 启动系统拍照，并预先创建缓存文件接收拍照结果。
     */
    private fun openCamera() {
        pendingCaptureSource = "camera"
        trackCaptureEvent(MonitoringNames.Events.receiptCaptureStart, source = pendingCaptureSource)
        val photoFile = File(requireContext().cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    /**
     * 打开系统图库选择图片。
     */
    private fun pickImageFromGallery() {
        pendingCaptureSource = "gallery"
        trackCaptureEvent(MonitoringNames.Events.receiptCaptureStart, source = pendingCaptureSource)
        pickImageLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    /**
     * 启动图片裁剪流程。
     *
     * @param sourceUri 拍照或相册选择得到的原始图片 URI
     */
    private fun startCrop(sourceUri: Uri) {
        val safeSource = resolveCropSourceUri(sourceUri) ?: run {
            trackCaptureEvent(
                eventName = MonitoringNames.Events.receiptCaptureFail,
                source = pendingCaptureSource,
                extraAttributes = mapOf(
                    MonitoringNames.Params.reason to "prepare_crop_source_failed"
                )
            )
            pendingCaptureSource = null
            Toast.makeText(requireContext(), getString(R.string.image_crop_failed), Toast.LENGTH_SHORT).show()
            return
        }
        val destination = Uri.fromFile(
            File(requireContext().cacheDir, "crop_${System.currentTimeMillis()}.jpg")
        )
        val options = UCrop.Options().apply {
            setToolbarTitle(getString(R.string.crop_image))
            setFreeStyleCropEnabled(true)
            setHideBottomControls(false)
        }
        val intent = UCrop.of(safeSource, destination)
            .withOptions(options)
            .getIntent(requireContext())
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        cropLauncher.launch(intent)
    }

    /**
     * 处理裁剪成功的图片结果，并把本地路径交给 ViewModel 发起上传识别。
     *
     * @param uri 裁剪后的输出文件 URI
     */
    private fun handleCroppedImage(uri: Uri) {
        val path = uri.path ?: return
        trackCaptureEvent(MonitoringNames.Events.receiptCaptureSuccess, source = pendingCaptureSource)
        pendingCaptureSource = null
        viewModel.processCroppedImage(path)
    }

    /**
     * 为裁剪组件准备可安全访问的输入 URI。
     *
     * 某些 `content://` URI 在第三方裁剪库中无法长期持有，这里会先拷贝到缓存文件。
     *
     * @param sourceUri 原始图片 URI
     * @return 可直接交给 UCrop 使用的 URI；失败时返回 `null`
     */
    private fun resolveCropSourceUri(sourceUri: Uri): Uri? {
        if (sourceUri.scheme != ContentResolver.SCHEME_CONTENT) return sourceUri
        return runCatching {
            val cacheFile = File(
                requireContext().cacheDir,
                "crop_source_${System.currentTimeMillis()}.jpg"
            )
            requireContext().contentResolver.openInputStream(sourceUri)?.use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            Uri.fromFile(cacheFile)
        }.onFailure { error ->
            LogHelper.e("Crop", "Failed to prepare crop source", error)
        }.getOrNull()
    }

    /**
     * 处理来自 ViewModel 的一次性事件。
     *
     * @param event 首页自定义事件
     */
    override fun onCustomEvent(event: UiEvent.Custom) {
        when (event.type) {
            HomeEventKeys.PREFILL_READY -> {
                val payload = event.payload ?: return
                val receipt = BundleCompat.getParcelable(
                    payload,
                    HomeEventKeys.EXTRA_ARGS,
                    ReceiptEntity::class.java
                )
                if (receipt != null) {
                    openInvoiceDetails(receipt)
                }
            }
            HomeEventKeys.SCAN_FAILED -> {
                ScanFailedDialog().show(parentFragmentManager, "scan_failed")
            }
        }
    }

    /**
     * 扫描成功后直接打开新增收据详情页。
     *
     * @param receipt OCR 识别得到的预填收据
     */
    private fun openInvoiceDetails(receipt: ReceiptEntity) {
        // 扫描成功后打开新增的收据填写详情
        // 标记来源为 SOURCE_SCAN，用于直接进入编辑状态（不经过预览）
        val intent = InvoiceDetailsActivity.createIntent(
            requireContext(),
            receipt,
            InvoiceDetailsArgsCodec.SOURCE_SCAN
        )
        invoiceDetailsLauncher.launch(intent)
    }

    /**
     * 把页面状态转换成 [StatefulListLayout] 需要的状态模型。
     *
     * @param state 当前首页 UI 状态
     * @return 列表容器可直接消费的状态对象
     */
    private fun buildListState(state: HomeUiState): StatefulListLayout.State {
        val contentState = when {
            state.loading && !state.hasLoaded -> StatefulListLayout.ContentState.LOADING
            !state.error.isNullOrBlank() && state.receipts.isEmpty() ->
                StatefulListLayout.ContentState.ERROR
            state.hasLoaded && state.empty -> StatefulListLayout.ContentState.EMPTY
            else -> StatefulListLayout.ContentState.CONTENT
        }
        val showNoMore = state.hasLoaded && !state.hasMore && state.receipts.isNotEmpty() && !state.loadingMore
        return StatefulListLayout.State(
            contentState = contentState,
            refreshing = state.refreshing,
            loadingMore = state.loadingMore,
            noMore = showNoMore,
            errorText = state.error
        )
    }

    /**
     * 仅统计首页首次可见且数据已就绪的首屏耗时。
     *
     * @param state 当前首页 UI 状态
     */
    private fun maybeTrackFirstScreenLoad(state: HomeUiState) {
        if (hasTrackedFirstScreenLoad || firstScreenStartedAtMs <= 0L) return
        if (state.loading || !state.hasLoaded) return

        hasTrackedFirstScreenLoad = true
        PerformanceMonitorManager.trackScreenLoad(
            screenName = MonitoringNames.Traces.homeFirstScreen,
            durationMs = SystemClock.elapsedRealtime() - firstScreenStartedAtMs
        )
    }

    /**
     * 统一上报拍照/选图相关埋点。
     *
     * @param eventName 事件名
     * @param source 来源，如 `camera` / `gallery`
     * @param extraAttributes 额外埋点属性
     */
    private fun trackCaptureEvent(
        eventName: String,
        source: String?,
        extraAttributes: Map<String, String> = emptyMap()
    ) {
        val attributes = buildMap {
            source?.let { put(MonitoringNames.Params.source, it) }
            putAll(extraAttributes)
        }
        TrackManager.track(eventName, attributes)
    }

    /**
     * 处理详情页返回结果并同步首页列表。
     *
     * @param operationType 详情页执行的操作类型：新增、更新、删除
     * @param receipt 操作后的收据对象；新增和更新成功时通常会返回
     * @param receiptId 删除场景下返回的收据 ID
     */
    private suspend fun handleInvoiceResult(
        operationType: String?,
        receipt: ReceiptEntity?,
        receiptId: Long
    ) {
        when (operationType) {
            InvoiceDetailsArgsCodec.OPERATION_TYPE_ADD -> {
                val hasValidId = receipt?.receiptId?.let { it > 0L } == true
                if (receipt != null && hasValidId) {
                    LogHelper.d(LOG_TAG, "invoice result add -> local insert + markReceiptsDirty")
                    viewModel.addReceiptLocally(receipt)
                    shouldScrollToTopOnNextRender = true
                    refreshViewModel.notifyReceiptsItemAdded(receipt)
                } else {
                    LogHelper.d(LOG_TAG, "invoice result add missing payload/id -> full refresh")
                    requestHomeRefreshOrDefer("invoice_details_result_add_missing_payload_or_id")
                    refreshViewModel.notifyReceiptsFullRefresh()
                }
            }
            InvoiceDetailsArgsCodec.OPERATION_TYPE_UPDATE -> {
                val hasValidId = receipt?.receiptId?.let { it > 0L } == true
                if (receipt != null && hasValidId) {
                    LogHelper.d(LOG_TAG, "invoice result update -> local update + markReceiptsDirty")
                    viewModel.updateReceiptLocally(receipt)
                    refreshViewModel.notifyReceiptsItemUpdated(receipt)
                } else {
                    LogHelper.d(LOG_TAG, "invoice result update missing payload/id -> full refresh")
                    requestHomeRefreshOrDefer("invoice_details_result_update_missing_payload_or_id")
                    refreshViewModel.notifyReceiptsFullRefresh()
                }
            }
            InvoiceDetailsArgsCodec.OPERATION_TYPE_DELETE -> {
                if (receiptId > 0L) {
                    LogHelper.d(LOG_TAG, "invoice result delete -> local delete + markReceiptsDirty")
                    viewModel.deleteReceiptLocally(receiptId)
                    refreshViewModel.notifyReceiptsItemDeleted(receiptId)
                } else {
                    refreshViewModel.notifyReceiptsFullRefresh()
                }
            }
            else -> {
                requestHomeRefreshOrDefer("invoice_details_result_unknown_operation")
                refreshViewModel.notifyReceiptsFullRefresh()
            }
        }
    }
}
