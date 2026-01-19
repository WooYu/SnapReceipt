package com.snapreceipt.io.ui.home

import android.app.Activity
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
import com.snapreceipt.io.ui.home.dialogs.EditReceiptDialog
import com.snapreceipt.io.ui.home.dialogs.ScanFailedDialog
import com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity
import com.skybound.space.base.presentation.BaseFragment
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.platform.permission.FragmentPermissionHelper
import com.skybound.space.base.platform.permission.PermissionManager
import com.skybound.space.base.platform.permission.Permissions
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

    private val permissionHelper by lazy { FragmentPermissionHelper(this) }

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
            Toast.makeText(requireContext(), getString(R.string.image_crop_failed), Toast.LENGTH_SHORT).show()
        }
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
        emptyState.visibility = if (state.empty) View.VISIBLE else View.GONE
        receiptList.visibility = if (state.empty) View.GONE else View.VISIBLE
        loadingIndicator.visibility = if (state.loading) View.VISIBLE else View.GONE
        scanCard.isEnabled = !state.loading
        uploadCard.isEnabled = !state.loading
    }

    private fun setupAdapter() {
        adapter = HomeReceiptAdapter { receipt ->
            showEditDialog(receipt)
        }
        receiptList.adapter = adapter
    }

    private fun setupListeners() {
        scanCard.setOnClickListener { openCameraWithPermission() }
        uploadCard.setOnClickListener { pickImageFromGallery() }
    }

    private fun showEditDialog(receipt: ReceiptEntity) {
        EditReceiptDialog(receipt) { updatedReceipt ->
            viewModel.updateReceipt(updatedReceipt)
            Toast.makeText(requireContext(), getString(R.string.success), Toast.LENGTH_SHORT).show()
        }.show(parentFragmentManager, "edit_receipt")
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
        val destination = Uri.fromFile(
            File(requireContext().cacheDir, "crop_${System.currentTimeMillis()}.jpg")
        )
        val options = UCrop.Options().apply {
            setToolbarTitle(getString(R.string.crop_image))
            setFreeStyleCropEnabled(true)
            setHideBottomControls(false)
        }
        val intent = UCrop.of(sourceUri, destination)
            .withOptions(options)
            .getIntent(requireContext())
        cropLauncher.launch(intent)
    }

    private fun handleCroppedImage(uri: Uri) {
        val path = uri.path ?: return
        viewModel.processCroppedImage(path)
    }

    override fun onCustomEvent(event: UiEvent.Custom) {
        when (event.type) {
            HomeEventKeys.PREFILL_READY -> {
                val imagePath = event.payload?.getString(HomeEventKeys.EXTRA_IMAGE_PATH).orEmpty()
                val imageUrl = event.payload?.getString(HomeEventKeys.EXTRA_IMAGE_URL).orEmpty()
                val merchant = event.payload?.getString(HomeEventKeys.EXTRA_MERCHANT).orEmpty()
                val amount = event.payload?.getString(HomeEventKeys.EXTRA_AMOUNT).orEmpty()
                val date = event.payload?.getString(HomeEventKeys.EXTRA_DATE).orEmpty()
                val time = event.payload?.getString(HomeEventKeys.EXTRA_TIME).orEmpty()
                val tipAmount = event.payload?.getString(HomeEventKeys.EXTRA_TIP_AMOUNT).orEmpty()
                val card = event.payload?.getString(HomeEventKeys.EXTRA_CARD).orEmpty()
                val consumer = event.payload?.getString(HomeEventKeys.EXTRA_CONSUMER).orEmpty()
                val remark = event.payload?.getString(HomeEventKeys.EXTRA_REMARK).orEmpty()
                openInvoiceDetails(
                    imagePath,
                    imageUrl,
                    merchant,
                    amount,
                    date,
                    time,
                    tipAmount,
                    card,
                    consumer,
                    remark
                )
            }
            HomeEventKeys.SCAN_FAILED -> {
                ScanFailedDialog().show(parentFragmentManager, "scan_failed")
            }
        }
    }

    private fun openInvoiceDetails(
        imagePath: String,
        imageUrl: String,
        merchant: String,
        amount: String,
        date: String,
        time: String,
        tipAmount: String,
        card: String,
        consumer: String,
        remark: String
    ) {
        val intent = android.content.Intent(requireContext(), InvoiceDetailsActivity::class.java)
        intent.putExtra(InvoiceDetailsActivity.EXTRA_IMAGE_PATH, imagePath)
        intent.putExtra(InvoiceDetailsActivity.EXTRA_IMAGE_URL, imageUrl)
        intent.putExtra(InvoiceDetailsActivity.EXTRA_MERCHANT, merchant)
        intent.putExtra(InvoiceDetailsActivity.EXTRA_AMOUNT, amount)
        intent.putExtra(InvoiceDetailsActivity.EXTRA_DATE, date)
        intent.putExtra(InvoiceDetailsActivity.EXTRA_TIME, time)
        intent.putExtra(InvoiceDetailsActivity.EXTRA_TIP_AMOUNT, tipAmount)
        intent.putExtra(InvoiceDetailsActivity.EXTRA_CARD, card)
        intent.putExtra(InvoiceDetailsActivity.EXTRA_CONSUMER, consumer)
        intent.putExtra(InvoiceDetailsActivity.EXTRA_NOTE, remark)
        startActivity(intent)
    }
}
