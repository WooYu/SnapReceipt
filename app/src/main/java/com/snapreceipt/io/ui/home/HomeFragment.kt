package com.snapreceipt.io.ui.home

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
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
import com.skybound.space.core.util.LogHelper
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.coroutines.Job

@AndroidEntryPoint
class HomeFragment : BaseFragment<HomeViewModel>(R.layout.fragment_home) {
    override val viewModel: HomeViewModel by viewModels()
    
    private val refreshViewModel: ListRefreshViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HomeReceiptAdapter
    private var pendingCameraUri: Uri? = null
    private var shouldScrollToTopOnNextRender = false
    private var pendingHomeRefreshAfterLoad = false
    private var refreshEventsJob: Job? = null

    private lateinit var permissionHelper: FragmentPermissionHelper

    // 用于启动 InvoiceDetailsActivity 并处理返回结果
    // 采用 ActivityResultContract 替代回调，实现现代化的结果传递
    private val invoiceDetailsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Activity 返回成功，获取操作类型和数据
            val operationType = result.data?.getStringExtra(InvoiceDetailsArgsCodec.EXTRA_OPERATION_TYPE)
            val receipt = result.data?.getParcelableExtra<ReceiptEntity>(InvoiceDetailsArgsCodec.EXTRA_RECEIPT)
            val receiptId = result.data?.getLongExtra(InvoiceDetailsArgsCodec.EXTRA_RECEIPT_ID, -1L)
            
            // 根据操作类型，选择本地快速更新或全量刷新
            lifecycleScope.launchWhenResumed {
                when (operationType) {
                    InvoiceDetailsArgsCodec.OPERATION_TYPE_ADD -> {
                        refreshViewModel.markReceiptsDirty()
                        // 新增：插入列表头部
                        if (receipt != null) {
                            viewModel.addReceiptLocally(receipt)
                            shouldScrollToTopOnNextRender = true
                            refreshViewModel.requestReceiptsItemAdded(receipt)
                        }
                    }
                    InvoiceDetailsArgsCodec.OPERATION_TYPE_UPDATE -> {
                        refreshViewModel.markReceiptsDirty()
                        // 编辑：更新列表中的项
                        if (receipt != null) {
                            viewModel.updateReceiptLocally(receipt)
                            // 后台增量刷新，确保服务端数据一致
                            refreshViewModel.requestReceiptsItemUpdated(receipt)
                        }
                    }
                    InvoiceDetailsArgsCodec.OPERATION_TYPE_DELETE -> {
                        refreshViewModel.markReceiptsDirty()
                        // 删除：移除列表中的项
                        if (receiptId != null && receiptId > 0) {
                            viewModel.deleteReceiptLocally(receiptId)
                            refreshViewModel.requestReceiptsItemDeleted(receiptId)
                        }
                    }
                    else -> {
                        // 未知操作或来自其他来源，执行全量刷新
                        requestHomeRefreshOrDefer()
                        refreshViewModel.requestReceiptsFullRefresh()
                        refreshViewModel.markReceiptsDirty()
                    }
                }
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            startCrop(uri)
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            startCrop(uri)
        }
    }

    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val output = UCrop.getOutput(result.data ?: return@registerForActivityResult)
            if (output != null) {
                handleCroppedImage(output)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val error = UCrop.getError(result.data ?: return@registerForActivityResult)
            LogHelper.e("Crop", "Crop failed", error)
            Toast.makeText(
                requireContext(),
                error?.localizedMessage ?: getString(R.string.image_crop_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = FragmentPermissionHelper(this)
    }

    override fun onResume() {
        super.onResume()
        refreshEventsJob?.cancel()

        if (refreshViewModel.consumeHomeDirty()) {
            requestHomeRefreshOrDefer()
        }

        // 监听来自 ListRefreshViewModel 的刷新事件
        // - ItemAdded: 扫描成功新增，本地已插入，这里处理后台增量刷新
        // - ItemUpdated: 编辑后更新，本地已更新，这里处理后台增量刷新
        // - ItemDeleted: 删除后，本地已移除，这里处理后台增量刷新
        // - FullRefresh: 全量刷新，重新加载整个列表
        refreshEventsJob = lifecycleScope.launchWhenResumed {
            refreshViewModel.refreshHomeEvent.collect { event ->
                when (event) {
                    is HomeRefreshEvent.ItemAdded -> {
                        viewModel.addReceiptLocally(event.receipt)
                    }
                    is HomeRefreshEvent.ItemUpdated -> {
                        viewModel.updateReceiptLocally(event.receipt)
                    }
                    is HomeRefreshEvent.ItemDeleted -> {
                        viewModel.deleteReceiptLocally(event.receiptId)
                    }
                    is HomeRefreshEvent.FullRefresh -> {
                        requestHomeRefreshOrDefer()
                    }
                }
            }
        }
    }

    override fun onPause() {
        refreshEventsJob?.cancel()
        refreshEventsJob = null
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)
        setupHeaderBackground()
        setupAdapter()
        setupListeners()
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

    private fun requestHomeRefreshOrDefer() {
        val currentState = viewModel.uiState.value
        if (!currentState.loading && !currentState.refreshing) {
            pendingHomeRefreshAfterLoad = false
            if (currentState.hasLoaded) {
                viewModel.refresh()
            } else {
                viewModel.loadReceipts()
            }
        } else {
            pendingHomeRefreshAfterLoad = true
        }
    }

    private fun tryConsumePendingHomeRefresh(state: HomeUiState) {
        if (!pendingHomeRefreshAfterLoad) return
        if (state.loading || state.refreshing) return
        pendingHomeRefreshAfterLoad = false
        if (state.hasLoaded) {
            viewModel.refresh()
        } else {
            viewModel.loadReceipts()
        }
    }

    private fun setupAdapter() {
        adapter = HomeReceiptAdapter { receipt ->
            openReceiptForEdit(receipt)
        }
        binding.statefulList.setAdapter(adapter)
        binding.statefulList.setOnRefreshListener { viewModel.refresh() }
        binding.statefulList.setOnLoadMoreListener { viewModel.loadMore() }
        binding.statefulList.setOnRetryListener { viewModel.loadReceipts() }
    }

    private fun setupListeners() {
        binding.cardScan.setOnClickListener { openCameraWithPermission() }
        binding.cardUpload.setOnClickListener { pickImageFromGallery() }
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
        pickImageLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    private fun startCrop(sourceUri: Uri) {
        val safeSource = resolveCropSourceUri(sourceUri) ?: run {
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
                val receipt = event.payload?.getParcelable(HomeEventKeys.EXTRA_ARGS) as? ReceiptEntity
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
}
