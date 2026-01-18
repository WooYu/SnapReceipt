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
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.UiEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InvoiceDetailsActivity : BaseActivity<InvoiceDetailsViewModel>() {
    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_MERCHANT = "extra_merchant"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_CARD = "extra_card"
        const val EXTRA_INVOICE_TYPE = "extra_invoice_type"
        const val EXTRA_TITLE_TYPE = "extra_title_type"
        const val EXTRA_NOTE = "extra_note"

        const val EXTRA_START_TAB = "extra_start_tab"
        const val TAB_RECEIPTS = "receipts"
    }

    override val viewModel: InvoiceDetailsViewModel by viewModels()

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
        if (imagePath.isNotEmpty()) {
            imageView.setImageURI(Uri.fromFile(java.io.File(imagePath)))
        }

        inputAmount.setText(intent.getStringExtra(EXTRA_AMOUNT).orEmpty())
        inputMerchant.setText(intent.getStringExtra(EXTRA_MERCHANT).orEmpty())
        inputAddress.setText(intent.getStringExtra(EXTRA_ADDRESS).orEmpty())
        inputDate.setText(intent.getStringExtra(EXTRA_DATE).orEmpty())
        inputCard.setText(intent.getStringExtra(EXTRA_CARD).orEmpty())
        inputInvoiceType.setText(intent.getStringExtra(EXTRA_INVOICE_TYPE).orEmpty())
        inputTitleType.setText(intent.getStringExtra(EXTRA_TITLE_TYPE).orEmpty())
        inputNote.setText(intent.getStringExtra(EXTRA_NOTE).orEmpty())

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btn_delete).setOnClickListener { finish() }

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
        val invoiceTypeValue = inputInvoiceType.text.toString().trim().ifEmpty { "normal" }
        val titleTypeValue = inputTitleType.text.toString().trim().ifEmpty { "Individual" }
        val noteValue = inputNote.text.toString().trim()

        val receipt = ReceiptEntity(
            merchantName = merchantValue,
            amount = amountValue,
            invoiceType = invoiceTypeValue,
            category = titleTypeValue,
            imagePath = imagePath,
            description = noteValue,
            date = System.currentTimeMillis()
        )

        viewModel.saveReceipt(receipt)
    }

    private fun navigateToMain() {
        val intent = Intent(this@InvoiceDetailsActivity, MainActivity::class.java)
        intent.putExtra(EXTRA_START_TAB, TAB_RECEIPTS)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}
