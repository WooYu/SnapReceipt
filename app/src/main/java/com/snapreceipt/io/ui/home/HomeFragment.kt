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
     * Renders UI state.
     * Data is submitted before state to prevent auto-scroll when footer appears.
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

    private fun setupListeners() {
        binding.cardScan.setOnClickListener { openCameraWithPermission() }
        binding.cardUpload.setOnClickListener { pickImageFromGallery() }
    }

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

    private fun pickImageFromGallery() {
        pendingCaptureSource = "gallery"
        trackCaptureEvent(MonitoringNames.Events.receiptCaptureStart, source = pendingCaptureSource)
        pickImageLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

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

    private fun handleCroppedImage(uri: Uri) {
        val path = uri.path ?: return
        trackCaptureEvent(MonitoringNames.Events.receiptCaptureSuccess, source = pendingCaptureSource)
        pendingCaptureSource = null
        viewModel.processCroppedImage(path)
    }

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

    private fun maybeTrackFirstScreenLoad(state: HomeUiState) {
        if (hasTrackedFirstScreenLoad || firstScreenStartedAtMs <= 0L) return
        if (state.loading || !state.hasLoaded) return

        hasTrackedFirstScreenLoad = true
        PerformanceMonitorManager.trackScreenLoad(
            screenName = MonitoringNames.Traces.homeFirstScreen,
            durationMs = SystemClock.elapsedRealtime() - firstScreenStartedAtMs
        )
    }

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
