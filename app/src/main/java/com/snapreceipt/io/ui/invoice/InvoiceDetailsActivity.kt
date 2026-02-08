package com.snapreceipt.io.ui.invoice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.observeState
import com.skybound.space.core.network.auth.SessionEvent
import com.skybound.space.core.network.auth.SessionManager
import com.skybound.space.core.util.DateFormatUtil
import com.snapreceipt.io.MainActivity
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityInvoiceDetailsBinding
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.ui.invoice.bottomsheet.DateTimePickerBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.InvoiceCategoryBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.TitleTypeBottomSheet
import com.snapreceipt.io.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
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
    private var hasSavedReceipt: Boolean = false
    private var isEditing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityInvoiceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        hasSavedReceipt = (receiptId ?: 0L) > 0L
        isEditing = !hasSavedReceipt
        if (receiptImagePath.isNotEmpty()) {
            binding.invoiceImage.setImageURI(Uri.fromFile(java.io.File(receiptImagePath)))
        } else if (receiptImageUrl.isNotEmpty()) {
            binding.invoiceImage.setImageURI(Uri.parse(receiptImageUrl))
        }

        val amountText = receipt.totalAmount?.let { formatAmount(it) }.orEmpty()
        binding.inputAmount.setText(amountText)
        binding.amountReadonlyValue.text = amountText.ifBlank { "0.00" }
        val merchantText = receipt.merchant.orEmpty()
        val addressText = receipt.address.orEmpty()
        binding.inputMerchant.setText(merchantText)
        binding.inputAddress.setText(addressText)
        binding.valueMerchant.text = merchantText
        binding.valueAddress.text = addressText
        receiptDate = receipt.receiptDate.orEmpty()
        receiptTime = receipt.receiptTime.orEmpty()
        val displayDate = buildDisplayDate(receiptDate, receiptTime)
        binding.inputDate.setText(displayDate)
        binding.valueDate.text = displayDate
        val cardText = receipt.paymentCardNo.orEmpty()
        binding.inputCard.setText(cardText)
        binding.valueCard.text = cardText
        scanConsumer = receipt.consumer.orEmpty()
        scanTipAmount = receipt.tipAmount
        val categoryLabel = receipt.categoryName.orEmpty()
        binding.inputInvoiceCategory.setText(categoryLabel)
        binding.valueInvoiceType.text = categoryLabel
        val titleType = receipt.receiptType.orEmpty()
        binding.inputTitleType.setText(titleType)
        binding.valueTitleType.text = titleType
        val noteText = receipt.remark.orEmpty()
        binding.inputNote.setText(noteText)
        binding.valueNote.text = noteText

        binding.pageTitle.setOnLeftIconClickListener { finish() }
        binding.btnDelete.setOnClickListener { onTopRightActionClick() }
        binding.invoiceImage.setOnClickListener { openImagePreview() }

        renderModeUi()
        setupPickers()
        setupCardValidation()
        binding.saveBtn.setOnClickListener { saveReceipt(receiptImagePath) }
        observeState(viewModel.uiState) { renderState(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
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

    private fun renderModeUi() {
        val showEdit = isEditing
        binding.amountEditContainer.visibility = if (showEdit) View.VISIBLE else View.GONE
        binding.amountReadonlyContainer.visibility = if (showEdit) View.GONE else View.VISIBLE
        binding.sectionEditMode.visibility = if (showEdit) View.VISIBLE else View.GONE
        binding.sectionViewMode.visibility = if (showEdit) View.GONE else View.VISIBLE
        binding.viewNoteLabel.visibility = if (showEdit) View.GONE else View.VISIBLE
        binding.valueNote.visibility = if (showEdit) View.GONE else View.VISIBLE
        binding.bottomActionContainer.visibility = if (showEdit) View.VISIBLE else View.GONE

        if (hasSavedReceipt) {
            binding.btnDelete.visibility = View.VISIBLE
            if (showEdit) {
                binding.btnDelete.setImageResource(R.drawable.ic_delete_outline_white)
                binding.btnDelete.contentDescription = getString(R.string.delete)
            } else {
                binding.btnDelete.setImageResource(R.drawable.ic_edit_white)
                binding.btnDelete.contentDescription = getString(R.string.edit_receipt)
            }
        } else {
            binding.btnDelete.visibility = View.GONE
        }
    }

    private fun onTopRightActionClick() {
        if (!hasSavedReceipt) return
        if (isEditing) {
            deleteReceiptIfNeeded()
            return
        }
        isEditing = true
        renderModeUi()
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
            Toast.makeText(this, getString(R.string.select_invoice_category), Toast.LENGTH_SHORT)
                .show()
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
            categoryName = invoiceCategoryInput,
            receiptType = titleTypeValue,
            address = binding.inputAddress.text.toString().trim()
        )
        if (hasSavedReceipt) {
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
            binding.valueInvoiceType.text = selected
        }.show(supportFragmentManager, "invoice_type_picker")
    }

    private fun openTitleTypePicker() {
        TitleTypeBottomSheet(binding.inputTitleType.text.toString()) { selected ->
            binding.inputTitleType.setText(selected)
            binding.valueTitleType.text = selected
        }.show(supportFragmentManager, "title_type_picker")
    }

    private fun openDateTimePicker() {
        val initial = parseDateTime(receiptDate, receiptTime)
        DateTimePickerBottomSheet(initial) { date, time, display ->
            receiptDate = date
            receiptTime = time
            binding.inputDate.setText(display)
            binding.valueDate.text = display
        }.show(supportFragmentManager, "date_time_picker")
    }

    private fun deleteReceiptIfNeeded() {
        if (!hasSavedReceipt) return
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

    private fun formatAmount(amount: Double): String =
        String.format(Locale.US, "%.2f", amount)

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
