package com.snapreceipt.io.ui.invoice

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.snapreceipt.io.MainActivity
import com.snapreceipt.io.R
import com.snapreceipt.io.data.db.ReceiptEntity
import com.snapreceipt.io.data.repository.ReceiptRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InvoiceDetailsActivity : AppCompatActivity() {
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

    @Inject lateinit var receiptRepository: ReceiptRepository

    private lateinit var imageView: ImageView
    private lateinit var inputAmount: EditText
    private lateinit var inputMerchant: EditText
    private lateinit var inputAddress: EditText
    private lateinit var inputDate: EditText
    private lateinit var inputCard: EditText
    private lateinit var inputInvoiceType: EditText
    private lateinit var inputTitleType: EditText
    private lateinit var inputNote: EditText

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

        findViewById<Button>(R.id.save_btn).setOnClickListener {
            saveReceipt(imagePath)
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
            description = noteValue
        )

        CoroutineScope(Dispatchers.IO).launch {
            receiptRepository.insertReceipt(receipt)
            runOnUiThread {
                Toast.makeText(this@InvoiceDetailsActivity, getString(R.string.success), Toast.LENGTH_SHORT).show()
                val intent = Intent(this@InvoiceDetailsActivity, MainActivity::class.java)
                intent.putExtra(EXTRA_START_TAB, TAB_RECEIPTS)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
        }
    }
}
