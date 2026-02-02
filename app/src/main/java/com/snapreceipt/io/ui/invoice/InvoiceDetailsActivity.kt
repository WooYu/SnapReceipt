package com.snapreceipt.io.ui.invoice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.snapreceipt.io.databinding.ActivityInvoiceDetailsBinding
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.observeState
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.core.network.auth.SessionEvent
import com.skybound.space.core.network.auth.SessionManager
import com.skybound.space.core.util.DateFormatUtil
import com.snapreceipt.io.MainActivity
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.ui.invoice.bottomsheet.DateTimePickerBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.InvoiceCategoryBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.TitleTypeBottomSheet
import com.snapreceipt.io.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InvoiceDetailsActivity : BaseActivity<InvoiceDetailsViewModel>() {
    companion object {
        const val EXTRA_ARGS = "extra_invoice_args"

        const val EXTRA_START_TAB = "extra_start_tab"
        const val TAB_RECEIPTS = "receipts"

        fun createIntent(context: Context, receipt: ReceiptEntity): Intent {
            return Intent(context, InvoiceDetailsActivity::class.java).apply {
                putExtra(EXTRA_ARGS, receipt)
            }
        }
    }

    override val viewModel: InvoiceDetailsViewModel by viewModels()

    @Inject
    lateinit var injectedSessionManager: SessionManager
    override val sessionManager: SessionManager
        get() = injectedSessionManager

    private var _binding: ActivityInvoiceDetailsBinding? = null
    private val binding get() = _binding!!

    private var receiptImagePath: String = ""
    private var receiptImageUrl: String = ""
    private var receiptDate: String = ""
    private var receiptTime: String = ""
    private var scanConsumer: String = ""
    private var scanTipAmount: Double? = null
    private var receiptId: Long? = null
    private var isEditMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice_details)

        imageView = findViewById(R.id.invoice_image)
        inputAmount = findViewById(R.id.input_amount)
        inputMerchant = findViewById(R.id.input_merchant)
        inputAddress = findViewById(R.id.input_address)
        inputDate = findViewById(R.id.input_date)
        inputCard = findViewById(R.id.input_card)
        inputInvoiceCategory = findViewById(R.id.input_invoice_category)
        inputTitleType = findViewById(R.id.input_title_type)
        inputNote = findViewById(R.id.input_note)
        cardHelper = findViewById(R.id.card_helper)
        saveButton = findViewById(R.id.save_btn)
        deleteButton = findViewById(R.id.btn_delete)

        val receipt = readReceipt(intent)
        val rawImage = receipt.receiptUrl.orEmpty()
        if (rawImage.startsWith("http", ignoreCase = true)) {
            receiptImageUrl = rawImage
            receiptImagePath = ""
        } else {
            receiptImagePath = rawImage
            receiptImageUrl = ""
        }
        receiptId = receipt.receiptId
        isEditMode = (receiptId ?: 0L) > 0L
        deleteButton.visibility =
            if (isEditMode) android.view.View.VISIBLE else android.view.View.GONE
        if (receiptImagePath.isNotEmpty()) {
            imageView.setImageURI(Uri.fromFile(java.io.File(receiptImagePath)))
        } else if (receiptImageUrl.isNotEmpty()) {
            imageView.setImageURI(Uri.parse(receiptImageUrl))
        }

        inputAmount.setText(receipt.totalAmount?.toString().orEmpty())
        inputMerchant.setText(receipt.merchant.orEmpty())
        inputAddress.setText(receipt.address.orEmpty())
        receiptDate = receipt.receiptDate.orEmpty()
        receiptTime = receipt.receiptTime.orEmpty()
        inputDate.setText(buildDisplayDate(receiptDate, receiptTime))
        inputCard.setText(receipt.paymentCardNo.orEmpty())
        scanConsumer = receipt.consumer.orEmpty()
        scanTipAmount = receipt.tipAmount
        val categoryLabel = receipt.categoryId?.let { ReceiptCategory.labelForId(it) }.orEmpty()
        inputInvoiceCategory.setText(categoryLabel)
        inputTitleType.setText(receipt.receiptType.orEmpty())
        inputNote.setText(receipt.remark.orEmpty())

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        deleteButton.setOnClickListener { deleteReceiptIfNeeded() }
        imageView.setOnClickListener { openImagePreview() }

        setupPickers()
        setupCardValidation()
        saveButton.setOnClickListener { saveReceipt(receiptImagePath) }
        observeState(viewModel.uiState) { renderState(it) }
    }

    private fun readReceipt(intent: Intent): ReceiptEntity {
        val receipt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ARGS, ReceiptEntity::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ARGS) as? ReceiptEntity
        }
        return receipt ?: ReceiptEntity()
    }

    private fun renderState(state: InvoiceDetailsUiState) {
        binding.saveBtn.isEnabled = !state.loading
    }

    override fun onCustomEvent(event: UiEvent.Custom) {
        when (event.type) {
            InvoiceDetailsEventKeys.SHOW_SUCCESS -> {
                Toast.makeText(this, getString(R.string.success), Toast.LENGTH_SHORT).show()
            }

            InvoiceDetailsEventKeys.NAVIGATE_TO_MAIN -> navigateToMain()
        }
    }

    private fun saveReceipt(imagePath: String) {
        val amountText = binding.inputAmount.text.toString().trim()
        val amountValue = amountText.toDoubleOrNull()
        val merchantValue = binding.inputMerchant.text.toString().trim()
            .ifEmpty { getString(R.string.receipt_default_name) }
        val invoiceCategoryInput = binding.inputInvoiceCategory.text.toString().trim()
        if (invoiceCategoryInput.isBlank()) {
            Toast.makeText(this, getString(R.string.select_invoice_category), Toast.LENGTH_SHORT)
                .show()
            return
        }
        val titleTypeValue = binding.inputTitleType.text.toString().trim()
        if (titleTypeValue.isBlank()) {
            Toast.makeText(this, getString(R.string.select_invoice_type), Toast.LENGTH_SHORT).show()
            return
        }
        val cardValue = binding.inputCard.text.toString().trim()
        val cardError = cardValidationErrorResId(cardValue)
        if (cardError != null) {
            updateCardHelper(cardValue)
            Toast.makeText(this, getString(cardError), Toast.LENGTH_SHORT).show()
            return
        }
        val noteValue = binding.inputNote.text.toString().trim()
        val categoryId = ReceiptCategory.idForLabel(invoiceCategoryInput)
        if (categoryId <= 0L) {
            Toast.makeText(this, getString(R.string.select_invoice_category), Toast.LENGTH_SHORT).show()
            return
        }
        val receiptUrl = receiptImageUrl.ifEmpty { imagePath }.takeIf { it.isNotBlank() }
        val safeDate = receiptDate.ifEmpty { currentDate() }
        val safeTime = receiptTime.ifEmpty { "00:00:00" }

        val receipt = ReceiptEntity(
            receiptId = receiptId,
            merchant = merchantValue,
            receiptDate = safeDate,
            receiptTime = safeTime,
            totalAmount = amountValue,
            tipAmount = scanTipAmount,
            paymentCardNo = cardValue,
            consumer = scanConsumer.ifEmpty { titleTypeValue },
            remark = noteValue,
            receiptUrl = receiptUrl,
            categoryId = categoryId,
            receiptType = titleTypeValue,
            address = binding.inputAddress.text.toString().trim()
        )
        if (isEditMode) {
            viewModel.updateReceipt(receipt)
        } else {
            viewModel.saveReceipt(receipt)
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this@InvoiceDetailsActivity, MainActivity::class.java)
        intent.putExtra(EXTRA_START_TAB, TAB_RECEIPTS)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    private fun setupPickers() {
        binding.inputInvoiceCategory.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { openInvoiceTypePicker() }
        }
        binding.inputTitleType.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { openTitleTypePicker() }
        }
        binding.inputDate.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { openDateTimePicker() }
        }
    }

    private fun setupCardValidation() {
        // TODO: temporarily disable validation to show backend values as-is.
        binding.cardHelper.visibility = View.GONE
    }

    private fun updateCardHelper(raw: String) {
        val errorRes = cardValidationErrorResId(raw)
        if (errorRes == null) {
            binding.cardHelper.visibility = View.GONE
            return
        }
        binding.cardHelper.text = getString(errorRes)
        binding.cardHelper.visibility = View.VISIBLE
    }

    private fun cardValidationErrorResId(raw: String): Int? {
        // TODO: validation rules will be re-enabled later.
        return null
    }

    private fun openImagePreview() {
        if (receiptImagePath.isBlank() && receiptImageUrl.isBlank()) return
        val intent =
            Intent(this, com.snapreceipt.io.ui.preview.ImagePreviewActivity::class.java).apply {
                putExtra(
                    com.snapreceipt.io.ui.preview.ImagePreviewActivity.EXTRA_IMAGE_PATH,
                    receiptImagePath
                )
                putExtra(
                    com.snapreceipt.io.ui.preview.ImagePreviewActivity.EXTRA_IMAGE_URL,
                    receiptImageUrl
                )
            }
        startActivity(intent)
    }

    private fun openInvoiceTypePicker() {
        InvoiceCategoryBottomSheet.newInstance(binding.inputInvoiceCategory.text.toString()) { selected ->
            binding.inputInvoiceCategory.setText(selected)
        }.show(supportFragmentManager, "invoice_type_picker")
    }

    private fun openTitleTypePicker() {
        TitleTypeBottomSheet(binding.inputTitleType.text.toString()) { selected ->
            binding.inputTitleType.setText(selected)
        }.show(supportFragmentManager, "title_type_picker")
    }

    private fun openDateTimePicker() {
        val initial = parseDateTime(receiptDate, receiptTime)
        DateTimePickerBottomSheet(initial) { date, time, display ->
            receiptDate = date
            receiptTime = time
            binding.inputDate.setText(display)
        }.show(supportFragmentManager, "date_time_picker")
    }

    private fun deleteReceiptIfNeeded() {
        if (!isEditMode) return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage(getString(R.string.delete_receipt_confirm))
            .setPositiveButton(R.string.confirm) { _, _ ->
                val id = receiptId ?: return@setPositiveButton
                viewModel.deleteReceipt(id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun buildDisplayDate(date: String, time: String): String {
        if (date.isBlank()) return ""
        val displayDate = date.replace('-', '/')
        val displayTime = time.takeIf { it.isNotBlank() }?.substring(0, 5).orEmpty()
        return if (displayTime.isNotEmpty()) "$displayDate $displayTime" else displayDate
    }

    private fun currentDate(): String =
        DateFormatUtil.todayApiDate()

    private fun parseDateTime(date: String, time: String): Long? =
        DateFormatUtil.parseApiDateTime(date, time)

    override fun onSessionExpired(event: SessionEvent) {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        finish()
    }
}
