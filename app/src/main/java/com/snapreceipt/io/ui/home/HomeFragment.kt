package com.snapreceipt.io.ui.home

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.ui.common.shouldShowEmpty
import com.snapreceipt.io.ui.home.dialogs.ScanFailedDialog
import com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity
import com.skybound.space.base.presentation.BaseFragment
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.platform.permission.FragmentPermissionHelper
import com.skybound.space.base.platform.permission.PermissionManager
import com.skybound.space.base.platform.permission.Permissions
import com.skybound.space.core.util.LogHelper
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class HomeFragment : BaseFragment<HomeViewModel>(R.layout.fragment_home) {
    override val viewModel: HomeViewModel by viewModels()

    private lateinit var receiptList: RecyclerView
    private lateinit var scanCard: MaterialCardView
    private lateinit var uploadCard: MaterialCardView
    private lateinit var previewCard: MaterialCardView
    private lateinit var previewImage: ImageView
    private lateinit var emptyState: View
    private lateinit var loadingIndicator: ProgressBar
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
                Toast.makeText(requireContext(), getString(R.string.image_crop_success), Toast.LENGTH_SHORT).show()
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
        receiptList = view.findViewById(R.id.receipt_list)
        scanCard = view.findViewById(R.id.card_scan)
        uploadCard = view.findViewById(R.id.card_upload)
        previewCard = view.findViewById(R.id.preview_card)
        previewImage = view.findViewById(R.id.preview_image)
        emptyState = view.findViewById(R.id.empty_state)
        loadingIndicator = view.findViewById(R.id.loading_indicator)

        setupAdapter()
        setupListeners()
        observeState()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadReceipts()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
    }

    private fun renderState(state: HomeUiState) {
        adapter.setReceipts(state.receipts)
        val showEmpty = shouldShowEmpty(state.hasLoaded, state.empty)
        emptyState.visibility = if (showEmpty) View.VISIBLE else View.GONE
        receiptList.visibility = if (showEmpty) View.GONE else View.VISIBLE
        loadingIndicator.visibility = if (state.loading) View.VISIBLE else View.GONE
        scanCard.isEnabled = !state.loading
        uploadCard.isEnabled = !state.loading
    }

    private fun setupAdapter() {
        adapter = HomeReceiptAdapter { receipt ->
            openReceiptForEdit(receipt)
        }
        receiptList.adapter = adapter
    }

    private fun setupListeners() {
        scanCard.setOnClickListener { openCameraWithPermission() }
        uploadCard.setOnClickListener { pickImageFromGallery() }
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
