package com.snapreceipt.io.ui.home

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.snapreceipt.io.R
import com.snapreceipt.io.data.db.ReceiptEntity
import com.snapreceipt.io.ui.home.dialogs.EditReceiptDialog
import com.skybound.space.base.platform.permission.FragmentPermissionHelper
import com.skybound.space.base.platform.permission.PermissionManager
import com.skybound.space.base.platform.permission.Permissions
import com.snapreceipt.io.ocr.OCRFactory
import com.snapreceipt.io.ocr.OCRMode
import com.snapreceipt.io.ocr.model.OCRResult
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var receiptList: RecyclerView
    private lateinit var scanCard: MaterialCardView
    private lateinit var uploadCard: MaterialCardView
    private lateinit var previewCard: MaterialCardView
    private lateinit var previewImage: ImageView
    private lateinit var adapter: ReceiptAdapter
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiptList = view.findViewById(R.id.receipt_list)
        scanCard = view.findViewById(R.id.card_scan)
        uploadCard = view.findViewById(R.id.card_upload)
        previewCard = view.findViewById(R.id.preview_card)
        previewImage = view.findViewById(R.id.preview_image)

        setupAdapter()
        setupListeners()
        observeData()
    }

    private fun setupAdapter() {
        adapter = ReceiptAdapter { receipt ->
            showEditDialog(receipt)
        }
        receiptList.adapter = adapter
    }

    private fun setupListeners() {
        scanCard.setOnClickListener { openCameraWithPermission() }
        uploadCard.setOnClickListener { pickImageFromGallery() }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.receipts.collect { receipts ->
                    if (receipts.isEmpty()) {
                        viewModel.seedSampleDataIfEmpty(buildSampleReceipts())
                    } else {
                        adapter.setReceipts(receipts)
                    }
                }
            }
        }
    }

    private fun buildSampleReceipts(): List<ReceiptEntity> {
        return listOf(
            ReceiptEntity(
                merchantName = "Starbucks Coffee",
                amount = 45.50,
                date = System.currentTimeMillis(),
                invoiceType = "normal",
                category = "Food & Beverage"
            ),
            ReceiptEntity(
                merchantName = "Apple Store",
                amount = 899.99,
                date = System.currentTimeMillis() - 86400000,
                invoiceType = "normal",
                category = "Electronics"
            ),
            ReceiptEntity(
                merchantName = "Gas Station",
                amount = 67.80,
                date = System.currentTimeMillis() - 172800000,
                invoiceType = "normal",
                category = "Transportation"
            )
        )
    }

    private fun showEditDialog(receipt: ReceiptEntity) {
        EditReceiptDialog(receipt) { updatedReceipt ->
            viewModel.updateReceipt(updatedReceipt)
            Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show()
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
        previewImage.setImageURI(uri)
        previewCard.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val ocrService = OCRFactory.create(OCRMode.MLKIT)
            val result = ocrService.recognizeImage(uri.path ?: return@launch)
            val prefill = buildReceiptPrefill(result, uri.path ?: return@launch)
            openInvoiceDetails(prefill)
        }
    }

    private fun buildReceiptPrefill(result: OCRResult?, imagePath: String): ReceiptPrefill {
        val text = result?.data?.get("text") as? String ?: ""
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val merchantName = lines.firstOrNull().orEmpty()

        val amountRegex = Regex("(\\d+[\\.,]\\d{2})")
        val amountMatch = amountRegex.find(text)
        val amount = amountMatch?.value?.replace(",", ".").orEmpty()

        return ReceiptPrefill(
            imagePath = imagePath,
            merchant = merchantName,
            amount = amount
        )
    }

    private fun openInvoiceDetails(prefill: ReceiptPrefill) {
        val intent = android.content.Intent(requireContext(), com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity::class.java)
        intent.putExtra(com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_IMAGE_PATH, prefill.imagePath)
        intent.putExtra(com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_MERCHANT, prefill.merchant)
        intent.putExtra(com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_AMOUNT, prefill.amount)
        startActivity(intent)
    }

    private data class ReceiptPrefill(
        val imagePath: String,
        val merchant: String,
        val amount: String
    )
}
