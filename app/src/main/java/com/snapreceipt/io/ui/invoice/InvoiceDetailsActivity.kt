package com.snapreceipt.io.ui.invoice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.core.network.auth.SessionEvent
import com.skybound.space.core.network.auth.SessionManager
import com.snapreceipt.io.MainActivity
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptSaveEntity
import com.snapreceipt.io.ui.invoice.bottomsheet.DateTimePickerBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.InvoiceCategoryBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.TitleTypeBottomSheet
import com.snapreceipt.io.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InvoiceDetailsActivity : BaseActivity<InvoiceDetailsViewModel>() {
    companion object {
        const val EXTRA_ARGS = "extra_invoice_args"

        const val EXTRA_START_TAB = "extra_start_tab"
        const val TAB_RECEIPTS = "receipts"

        fun createIntent(context: Context, args: InvoiceDetailsArgs): Intent {
            return Intent(context, InvoiceDetailsActivity::class.java).apply {
                putExtra(EXTRA_ARGS, args)
            }
        }
    }

    override val viewModel: InvoiceDetailsViewModel by viewModels()

    @Inject
    lateinit var injectedSessionManager: SessionManager
    override val sessionManager: SessionManager
        get() = injectedSessionManager

    private lateinit var imageView: ImageView
    private lateinit var inputAmount: EditText
    private lateinit var inputMerchant: EditText
    private lateinit var inputAddress: EditText
    private lateinit var inputDate: EditText
    private lateinit var inputCard: EditText
    private lateinit var inputInvoiceCategory: EditText
    private lateinit var inputTitleType: EditText
    private lateinit var inputNote: EditText
    private lateinit var cardHelper: TextView
    private lateinit var saveButton: Button
    private lateinit var deleteButton: ImageView
    private var receiptImagePath: String = ""
    private var receiptImageUrl: String = ""
    private var receiptDate: String = ""
    private var receiptTime: String = ""
    private var scanConsumer: String = ""
    private var scanTipAmount: Double = 0.0
    private var receiptId: Long = 0L
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

        val args = intent.getParcelableExtra(EXTRA_ARGS) as? InvoiceDetailsArgs ?: InvoiceDetailsArgs()
        receiptImagePath = args.imagePath
        receiptImageUrl = args.imageUrl
        receiptId = args.receiptId
        isEditMode = receiptId > 0L
        deleteButton.visibility =
            if (isEditMode) android.view.View.VISIBLE else android.view.View.GONE
        if (receiptImagePath.isNotEmpty()) {
            imageView.setImageURI(Uri.fromFile(java.io.File(receiptImagePath)))
        } else if (receiptImageUrl.isNotEmpty()) {
            imageView.setImageURI(Uri.parse(receiptImageUrl))
        }

        inputAmount.setText(args.amount?.toString().orEmpty())
        inputMerchant.setText(args.merchant)
        inputAddress.setText(args.address)
        receiptDate = args.date
        receiptTime = args.time
        inputDate.setText(buildDisplayDate(receiptDate, receiptTime))
        inputCard.setText(args.card)
        scanConsumer = args.consumer
        scanTipAmount = args.tipAmount ?: 0.0
        inputInvoiceCategory.setText(args.invoiceCategory)
        inputTitleType.setText(args.titleType)
        inputNote.setText(args.note)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        deleteButton.setOnClickListener { deleteReceiptIfNeeded() }
        imageView.setOnClickListener { openImagePreview() }

        setupPickers()
        setupCardValidation()
        saveButton.setOnClickListener { saveReceipt(receiptImagePath) }
        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
    }

    private fun renderState(state: InvoiceDetailsUiState) {
        if (::saveButton.isInitialized) {
            saveButton.isEnabled = !state.loading
        }
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
        val amountValue = inputAmount.text.toString().trim().toDoubleOrNull() ?: 0.0
        val merchantValue = inputMerchant.text.toString().trim()
            .ifEmpty { getString(R.string.receipt_default_name) }
        val invoiceCategoryInput = inputInvoiceCategory.text.toString().trim()
        if (invoiceCategoryInput.isBlank()) {
            Toast.makeText(this, getString(R.string.select_invoice_category), Toast.LENGTH_SHORT)
                .show()
            return
        }
        val titleTypeValue =
            inputTitleType.text.toString().trim()
        if (titleTypeValue.isBlank()) {
            Toast.makeText(this, getString(R.string.select_invoice_type), Toast.LENGTH_SHORT).show()
            return
        }
        val cardValue = inputCard.text.toString().trim()
        val cardError = cardValidationErrorResId(cardValue)
        if (cardError != null) {
            updateCardHelper(cardValue)
            Toast.makeText(this, getString(cardError), Toast.LENGTH_SHORT).show()
            return
        }
        val noteValue = inputNote.text.toString().trim()
        val categoryId = ReceiptCategory.idForLabel(invoiceCategoryInput)
        if (categoryId <= 0) {
            Toast.makeText(this, getString(R.string.select_invoice_category), Toast.LENGTH_SHORT).show()
            return
        }
        val receiptUrl = receiptImageUrl.ifEmpty { imagePath }
        val safeDate = receiptDate.ifEmpty { currentDate() }
        val safeTime = receiptTime.ifEmpty { "00:00:00" }

        if (isEditMode) {
            val updateRequest = com.snapreceipt.io.domain.model.ReceiptUpdateEntity(
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
                categoryId = categoryId
            )
            viewModel.updateReceipt(updateRequest)
        } else {
            val request = ReceiptSaveEntity(
                merchant = merchantValue,
                receiptDate = safeDate,
                receiptTime = safeTime,
                totalAmount = amountValue,
                tipAmount = scanTipAmount,
                paymentCardNo = cardValue,
                consumer = scanConsumer.ifEmpty { titleTypeValue },
                remark = noteValue,
                receiptUrl = receiptUrl,
                categoryId = categoryId
            )
            viewModel.saveReceipt(request)
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
        if (inputInvoiceCategory.text.isNullOrBlank()) {
            inputInvoiceCategory.setText(
                ReceiptCategory.all().firstOrNull()?.label ?: getString(R.string.type_other)
            )
        }
        if (inputTitleType.text.isNullOrBlank()) {
            inputTitleType.setText(getString(R.string.type_individual))
        }
        inputInvoiceCategory.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { openInvoiceTypePicker() }
        }
        inputTitleType.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { openTitleTypePicker() }
        }
        inputDate.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener { openDateTimePicker() }
        }
    }

    private fun setupCardValidation() {
        // TODO: temporarily disable validation to show backend values as-is.
        cardHelper.visibility = View.GONE
    }

    private fun updateCardHelper(raw: String) {
        val errorRes = cardValidationErrorResId(raw)
        if (errorRes == null) {
            cardHelper.visibility = View.GONE
            return
        }
        cardHelper.text = getString(errorRes)
        cardHelper.visibility = View.VISIBLE
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
        InvoiceCategoryBottomSheet.newInstance(inputInvoiceCategory.text.toString()) { selected ->
            inputInvoiceCategory.setText(selected)
        }.show(supportFragmentManager, "invoice_type_picker")
    }

    private fun openTitleTypePicker() {
        TitleTypeBottomSheet(inputTitleType.text.toString()) { selected ->
            inputTitleType.setText(selected)
        }.show(supportFragmentManager, "title_type_picker")
    }

    private fun openDateTimePicker() {
        val initial = parseDateTime(receiptDate, receiptTime)
        DateTimePickerBottomSheet(initial) { date, time, display ->
            receiptDate = date
            receiptTime = time
            inputDate.setText(display)
        }.show(supportFragmentManager, "date_time_picker")
    }

    private fun deleteReceiptIfNeeded() {
        if (!isEditMode) return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage(getString(R.string.delete_receipt_confirm))
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.deleteReceipt(receiptId)
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

    private fun currentDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(System.currentTimeMillis())
    }

    private fun parseDateTime(date: String, time: String): Long? {
        if (date.isBlank()) return null
        val safeTime = if (time.isBlank()) "00:00:00" else time
        val value = "$date $safeTime"
        return runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .parse(value)
                ?.time
        }.getOrNull()
    }

    override fun onSessionExpired(event: SessionEvent) {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        finish()
    }
}
