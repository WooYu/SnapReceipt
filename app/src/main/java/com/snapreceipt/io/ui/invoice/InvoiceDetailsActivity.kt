package com.snapreceipt.io.ui.invoice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
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
import com.snapreceipt.io.ui.receipts.ReceiptsRefreshSignal
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class InvoiceDetailsActivity : BaseActivity<InvoiceDetailsViewModel>() {
    companion object {
        const val EXTRA_START_TAB = "extra_start_tab"
        const val TAB_RECEIPTS = "receipts"
        private const val RECEIPT_TYPE_BUSINESS = "Business"
        private const val RECEIPT_TYPE_INDIVIDUAL = "Individual"

        fun createIntent(context: Context, receipt: ReceiptEntity): Intent {
            return Intent(context, InvoiceDetailsActivity::class.java).apply {
                InvoiceDetailsArgsCodec.writeReceipt(this, receipt)
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
        applyWindowInsets()

        val receipt = InvoiceDetailsArgsCodec.readReceipt(intent)
        val rawImage = receipt.receiptUrl.orEmpty().trim()
        resolveImageSource(rawImage)
        receiptId = receipt.receiptId
        hasSavedReceipt = (receiptId ?: 0L) > 0L
        isEditing = !hasSavedReceipt
        loadInvoiceImage()

        val amountText = receipt.totalAmount?.let { formatAmount(it) }.orEmpty()
        binding.inputAmount.setText(amountText)
        bindReadonlyAmount(amountText)
        val merchantText = receipt.merchant.orEmpty()
        val addressText = receipt.address.orEmpty()
        binding.inputMerchant.setText(merchantText)
        binding.inputAddress.setText(addressText)
        binding.valueMerchant.text = readonlyText(merchantText)
        binding.valueAddress.text = readonlyText(addressText)
        receiptDate = receipt.receiptDate.orEmpty()
        receiptTime = receipt.receiptTime.orEmpty()
        val displayDate = buildDisplayDate(receiptDate, receiptTime)
        binding.inputDate.setText(displayDate)
        binding.valueDate.text = readonlyText(displayDate)
        val cardText = receipt.paymentCardNo.orEmpty()
        binding.inputCard.setText(cardText)
        binding.valueCard.text = readonlyText(cardText)
        scanConsumer = receipt.consumer.orEmpty()
        scanTipAmount = receipt.tipAmount
        setInvoiceCategorySelection(receipt.categoryName.orEmpty())
        setTitleTypeSelection(receipt.receiptType.orEmpty())
        val noteText = receipt.remark.orEmpty()
        binding.inputNote.setText(noteText)
        binding.valueNote.text = readonlyText(noteText)

        binding.pageTitle.setOnLeftIconClickListener { finish() }
        binding.pageTitle.setOnRightIconClickListener { onTopRightActionClick() }
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

    private fun renderState(state: InvoiceDetailsUiState) {
        binding.saveBtn.isEnabled = !state.loading
        if (state.loading) {
            showGlobalLoading(null)
        } else {
            hideGlobalLoading()
        }
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

        binding.pageTitle.setRightIconVisible(hasSavedReceipt)
        if (hasSavedReceipt) {
            if (showEdit) {
                binding.pageTitle.setRightIcon(R.drawable.ic_trash_white)
            } else {
                binding.pageTitle.setRightIcon(R.drawable.ic_edit_white)
            }
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun applyWindowInsets() {
        val initialScrollBottomPadding = binding.contentScroll.paddingBottom
        val initialBottomActionPadding = binding.bottomActionContainer.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navigationBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomInset = max(navigationBottom, imeBottom)

            val scrollBottomInset = if (binding.bottomActionContainer.visibility == View.VISIBLE) {
                0
            } else {
                bottomInset
            }
            binding.contentScroll.updatePadding(bottom = initialScrollBottomPadding + scrollBottomInset)
            binding.bottomActionContainer.updatePadding(bottom = initialBottomActionPadding + bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
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
                Toast.makeText(this, getString(R.string.receipt_save_success), Toast.LENGTH_SHORT).show()
            }

            InvoiceDetailsEventKeys.NAVIGATE_TO_MAIN -> {
                ReceiptsRefreshSignal.requestRefresh()
                navigateToMain()
            }
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
        val titleTypeDisplayValue = binding.inputTitleType.text.toString().trim()
        val titleTypeValue = toReceiptTypePayloadValue(titleTypeDisplayValue)
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
        bindPickerRow(
            container = binding.containerInputInvoiceCategory,
            input = binding.inputInvoiceCategory,
            onClick = ::openInvoiceCategoryPicker
        )
        bindPickerRow(
            container = binding.containerInputTitleType,
            input = binding.inputTitleType,
            onClick = ::openTitleTypePicker
        )
        bindPickerRow(
            container = binding.containerInputDate,
            input = binding.inputDate,
            onClick = ::openDateTimePicker
        )
    }

    private fun bindPickerRow(
        container: View,
        input: View,
        onClick: () -> Unit
    ) {
        container.setOnClickListener { onClick() }
        input.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { onClick() }
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

    private fun openInvoiceCategoryPicker() {
        InvoiceCategoryBottomSheet.newInstance(binding.inputInvoiceCategory.text.toString()) { selected ->
            setInvoiceCategorySelection(selected)
        }.show(supportFragmentManager, "invoice_category_picker")
    }

    private fun openTitleTypePicker() {
        TitleTypeBottomSheet(binding.inputTitleType.text.toString()) { selected ->
            setTitleTypeSelection(selected)
        }.show(supportFragmentManager, "title_type_picker")
    }

    private fun setInvoiceCategorySelection(value: String) {
        val normalized = value.trim()
        binding.inputInvoiceCategory.setText(normalized)
        binding.valueInvoiceType.text = readonlyText(normalized)
    }

    private fun setTitleTypeSelection(value: String) {
        val displayLabel = toReceiptTypeDisplayLabel(value)
        binding.inputTitleType.setText(displayLabel)
        binding.valueTitleType.text = readonlyText(displayLabel)
    }

    private fun toReceiptTypeDisplayLabel(raw: String): String {
        val normalized = raw.trim()
        if (normalized.isBlank()) return ""
        val companyLabel = getString(R.string.type_company)
        val individualLabel = getString(R.string.type_individual)
        return when {
            normalized.equals(RECEIPT_TYPE_BUSINESS, ignoreCase = true) ||
                normalized.equals(companyLabel, ignoreCase = true) -> companyLabel

            normalized.equals(RECEIPT_TYPE_INDIVIDUAL, ignoreCase = true) ||
                normalized.equals(individualLabel, ignoreCase = true) -> individualLabel

            else -> normalized
        }
    }

    private fun toReceiptTypePayloadValue(raw: String): String {
        val normalized = raw.trim()
        if (normalized.isBlank()) return ""
        val companyLabel = getString(R.string.type_company)
        val individualLabel = getString(R.string.type_individual)
        return when {
            normalized.equals(companyLabel, ignoreCase = true) ||
                normalized.equals(RECEIPT_TYPE_BUSINESS, ignoreCase = true) -> RECEIPT_TYPE_BUSINESS

            normalized.equals(individualLabel, ignoreCase = true) ||
                normalized.equals(RECEIPT_TYPE_INDIVIDUAL, ignoreCase = true) -> RECEIPT_TYPE_INDIVIDUAL

            else -> normalized
        }
    }

    private fun openDateTimePicker() {
        val initial = parseDateTime(receiptDate, receiptTime)
        DateTimePickerBottomSheet(initial) { date, time, display ->
            receiptDate = date
            receiptTime = time
            binding.inputDate.setText(display)
            binding.valueDate.text = readonlyText(display)
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

    private fun bindReadonlyAmount(amountText: String) {
        val hasAmount = amountText.isNotBlank()
        binding.amountReadonlyValue.text = if (hasAmount) amountText else placeholder()
        binding.amountPrefixView.visibility = if (hasAmount) View.VISIBLE else View.GONE
    }

    private fun placeholder(): String = getString(R.string.placeholder_dash)

    private fun readonlyText(raw: String): String = raw.ifBlank { placeholder() }

    private fun resolveImageSource(rawImage: String) {
        if (rawImage.isBlank()) {
            receiptImagePath = ""
            receiptImageUrl = ""
            return
        }
        if (rawImage.startsWith("http", ignoreCase = true)) {
            receiptImageUrl = rawImage
            receiptImagePath = ""
            return
        }
        receiptImagePath = rawImage
        receiptImageUrl = ""
    }

    private fun loadInvoiceImage() {
        val model = imageModel(receiptImagePath, receiptImageUrl) ?: return
        Glide.with(this)
            .load(model)
            .centerCrop()
            .into(binding.invoiceImage)
    }

    private fun imageModel(imagePath: String, imageUrl: String): Any? {
        localImageModel(imagePath)?.let { return it }
        return imageUrl.takeIf { it.isNotBlank() }
    }

    private fun localImageModel(path: String): Any? {
        if (path.isBlank()) return null
        if (path.startsWith("content://", ignoreCase = true) ||
            path.startsWith("file://", ignoreCase = true)
        ) {
            return Uri.parse(path)
        }
        return File(path).takeIf { it.exists() }
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
