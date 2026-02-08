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
import androidx.fragment.app.viewModels
import androidx.core.content.ContextCompat
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.FragmentHomeBinding
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.ui.home.dialogs.ScanFailedDialog
import com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity
import com.snapreceipt.io.ui.widget.CurvedGradientDrawable
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

@AndroidEntryPoint
class HomeFragment : BaseFragment<HomeViewModel>(R.layout.fragment_home) {
    override val viewModel: HomeViewModel by viewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HomeReceiptAdapter
    private var pendingCameraUri: Uri? = null

    private lateinit var permissionHelper: FragmentPermissionHelper

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
        val curveHeight = resources.displayMetrics.density * 45f // 45dp 转换为像素
        binding.headerBg.background = CurvedGradientDrawable(startColor, endColor, curveHeight)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderState(state: HomeUiState) {
        adapter.setReceipts(state.receipts)
        binding.statefulList.applyState(
            hasLoaded = state.hasLoaded,
            isEmpty = state.empty,
            hasMore = state.hasMore,
            itemCount = state.receipts.size,
            refreshing = state.refreshing,
            loadingMore = state.loadingMore
        )

        val showRecognitionOverlay = state.recognitionStatusResId != null
        if (showRecognitionOverlay) {
            binding.recognitionOverlay.show(state.recognitionStatusResId!!)
        } else {
            binding.recognitionOverlay.hide()
        }
        (activity as? com.snapreceipt.io.MainActivity)?.setBottomNavVisible(!showRecognitionOverlay)
        showLoading(state.loading && !showRecognitionOverlay)

        binding.cardScan.isEnabled = !state.loading
        binding.cardUpload.isEnabled = !state.loading
    }

    private fun setupAdapter() {
        adapter = HomeReceiptAdapter { receipt ->
            openReceiptForEdit(receipt)
        }
        binding.statefulList.setAdapter(adapter)
        binding.statefulList.setOnLoadMoreListener { viewModel.loadMore() }
    }

    private fun setupListeners() {
        binding.cardScan.setOnClickListener { openCameraWithPermission() }
        binding.cardUpload.setOnClickListener { pickImageFromGallery() }
        binding.statefulList.setOnRefreshListener { viewModel.refresh() }
    }

    private fun openReceiptForEdit(receipt: ReceiptEntity) {
        startActivity(InvoiceDetailsActivity.createIntent(requireContext(), receipt))
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
        startActivity(InvoiceDetailsActivity.createIntent(requireContext(), receipt))
    }
}
