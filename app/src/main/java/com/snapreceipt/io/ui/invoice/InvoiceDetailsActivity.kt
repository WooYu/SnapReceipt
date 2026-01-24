package com.snapreceipt.io.ui.invoice

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.snapreceipt.io.MainActivity
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptSaveEntity
import com.snapreceipt.io.ui.invoice.bottomsheet.DateTimePickerBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.InvoiceTypeBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.TitleTypeBottomSheet
import com.snapreceipt.io.ui.login.LoginActivity
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.core.network.auth.SessionEvent
import com.skybound.space.core.network.auth.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InvoiceDetailsActivity : BaseActivity<InvoiceDetailsViewModel>() {
    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_IMAGE_URL = "extra_image_url"
        const val EXTRA_MERCHANT = "extra_merchant"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_CARD = "extra_card"
        const val EXTRA_CONSUMER = "extra_consumer"
        const val EXTRA_TIP_AMOUNT = "extra_tip_amount"
        const val EXTRA_INVOICE_TYPE = "extra_invoice_type"
        const val EXTRA_TITLE_TYPE = "extra_title_type"
        const val EXTRA_NOTE = "extra_note"

        const val EXTRA_START_TAB = "extra_start_tab"
        const val TAB_RECEIPTS = "receipts"
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
    private lateinit var inputInvoiceType: EditText
    private lateinit var inputTitleType: EditText
    private lateinit var inputNote: EditText
    private lateinit var saveButton: Button
    private var receiptImageUrl: String = ""
    private var receiptDate: String = ""
    private var receiptTime: String = ""
    private var scanConsumer: String = ""
    private var scanTipAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice_details)

        imageView = findViewById(R.id.invoice_image)
        inputAmount = findViewById(R.id.input_amount)
        inputMerchant = findViewById(R.id.input_merchant)
        inputAddress = findViewById(R.id.input_address)
        inputDate = findViewById(R.id.input_date)
        inputCard = findViewById(R.id.input_card)
        inputInvoiceType = findViewById(R.id.input_invoice_type)
        inputTitleType = findViewById(R.id.input_title_type)
        inputNote = findViewById(R.id.input_note)
        saveButton = findViewById(R.id.save_btn)

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH).orEmpty()
        receiptImageUrl = intent.getStringExtra(EXTRA_IMAGE_URL).orEmpty()
        if (imagePath.isNotEmpty()) {
            imageView.setImageURI(Uri.fromFile(java.io.File(imagePath)))
        } else if (receiptImageUrl.isNotEmpty()) {
            imageView.setImageURI(Uri.parse(receiptImageUrl))
        }

        inputAmount.setText(intent.getStringExtra(EXTRA_AMOUNT).orEmpty())
        inputMerchant.setText(intent.getStringExtra(EXTRA_MERCHANT).orEmpty())
        inputAddress.setText(intent.getStringExtra(EXTRA_ADDRESS).orEmpty())
        receiptDate = intent.getStringExtra(EXTRA_DATE).orEmpty()
        receiptTime = intent.getStringExtra(EXTRA_TIME).orEmpty()
        inputDate.setText(buildDisplayDate(receiptDate, receiptTime))
        inputCard.setText(intent.getStringExtra(EXTRA_CARD).orEmpty())
        scanConsumer = intent.getStringExtra(EXTRA_CONSUMER).orEmpty()
        scanTipAmount = intent.getStringExtra(EXTRA_TIP_AMOUNT)?.toDoubleOrNull() ?: 0.0
        inputInvoiceType.setText(intent.getStringExtra(EXTRA_INVOICE_TYPE).orEmpty())
        inputTitleType.setText(intent.getStringExtra(EXTRA_TITLE_TYPE).orEmpty())
        inputNote.setText(intent.getStringExtra(EXTRA_NOTE).orEmpty())

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btn_delete).setOnClickListener { finish() }

        setupPickers()
        saveButton.setOnClickListener { saveReceipt(imagePath) }
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
        val merchantValue = inputMerchant.text.toString().trim().ifEmpty { "Receipt" }
        val invoiceTypeInput = inputInvoiceType.text.toString().trim()
        val invoiceTypeValue = if (invoiceTypeInput.equals(getString(R.string.type_all), true) || invoiceTypeInput.isBlank()) {
            ReceiptCategory.all().firstOrNull()?.label ?: "Food"
        } else {
            invoiceTypeInput
        }
        val titleTypeValue = inputTitleType.text.toString().trim().ifEmpty { "Individual" }
        val cardValue = inputCard.text.toString().trim()
        val noteValue = inputNote.text.toString().trim()
        val categoryId = ReceiptCategory.idForLabel(invoiceTypeValue).takeIf { it > 0 } ?: 1
        val receiptUrl = if (receiptImageUrl.isNotEmpty()) receiptImageUrl else imagePath
        val safeDate = receiptDate.ifEmpty { currentDate() }
        val safeTime = receiptTime.ifEmpty { "00:00:00" }

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

    private fun navigateToMain() {
        val intent = Intent(this@InvoiceDetailsActivity, MainActivity::class.java)
        intent.putExtra(EXTRA_START_TAB, TAB_RECEIPTS)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }

    private fun setupPickers() {
        if (inputInvoiceType.text.isNullOrBlank()) {
            inputInvoiceType.setText(ReceiptCategory.all().firstOrNull()?.label ?: "")
        }
        if (inputTitleType.text.isNullOrBlank()) {
            inputTitleType.setText(getString(R.string.type_individual))
        }
        inputInvoiceType.apply {
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

    private fun openInvoiceTypePicker() {
        InvoiceTypeBottomSheet.newInstance(inputInvoiceType.text.toString()) { selected ->
            inputInvoiceType.setText(selected)
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
