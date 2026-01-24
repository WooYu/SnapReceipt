package com.snapreceipt.io.ui.receipts

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.ui.home.dialogs.EditReceiptDialog
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ReceiptDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_receipt_id"
        const val EXTRA_MERCHANT = "extra_receipt_merchant"
        const val EXTRA_AMOUNT = "extra_receipt_amount"
        const val EXTRA_CATEGORY = "extra_receipt_category"
        const val EXTRA_INVOICE_TYPE = "extra_receipt_invoice_type"
        const val EXTRA_DATE = "extra_receipt_date"
        const val EXTRA_IMAGE_PATH = "extra_receipt_image_path"
        const val EXTRA_NOTE = "extra_receipt_note"
        const val EXTRA_ADDRESS = "extra_receipt_address"
        const val EXTRA_CARD = "extra_receipt_card"
    }

    private val viewModel: ReceiptsViewModel by viewModels()
    private var currentReceipt: ReceiptEntity? = null

    private lateinit var receiptImage: ImageView
    private lateinit var merchantValue: TextView
    private lateinit var addressValue: TextView
    private lateinit var dateValue: TextView
    private lateinit var cardValue: TextView
    private lateinit var invoiceTypeValue: TextView
    private lateinit var titleTypeValue: TextView
    private lateinit var noteValue: TextView
    private lateinit var amountValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_details)

        receiptImage = findViewById(R.id.receipt_image)
        merchantValue = findViewById(R.id.merchant_value)
        addressValue = findViewById(R.id.address_value)
        dateValue = findViewById(R.id.date_value)
        cardValue = findViewById(R.id.card_value)
        invoiceTypeValue = findViewById(R.id.invoice_type_value)
        titleTypeValue = findViewById(R.id.title_type_value)
        noteValue = findViewById(R.id.note_value)
        amountValue = findViewById(R.id.total_amount_value)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btn_edit).setOnClickListener { openEditDialog() }

        bindReceipt(loadReceiptFromIntent())
    }

    private fun loadReceiptFromIntent(): ReceiptEntity {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val merchant = intent.getStringExtra(EXTRA_MERCHANT).orEmpty()
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val category = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        val invoiceType = intent.getStringExtra(EXTRA_INVOICE_TYPE).orEmpty()
        val date = intent.getLongExtra(EXTRA_DATE, System.currentTimeMillis())
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH).orEmpty()
        val note = intent.getStringExtra(EXTRA_NOTE).orEmpty()

        val receipt = ReceiptEntity(
            id = id,
            merchantName = merchant,
            amount = amount,
            category = category,
            invoiceType = invoiceType.ifBlank { getString(R.string.type_individual) },
            date = date,
            imagePath = imagePath,
            description = note
        )
        currentReceipt = receipt
        return receipt
    }

    private fun bindReceipt(receipt: ReceiptEntity) {
        merchantValue.text = receipt.merchantName
        invoiceTypeValue.text = receipt.category.ifBlank { getString(R.string.type_other) }
        titleTypeValue.text = receipt.invoiceType.ifBlank { getString(R.string.type_individual) }
        noteValue.text = receipt.description.ifBlank { getString(R.string.placeholder_dash) }
        amountValue.text = getString(R.string.amount_number_format, receipt.amount)
        dateValue.text = formatDisplayDate(receipt.date)

        val address = intent.getStringExtra(EXTRA_ADDRESS).orEmpty()
        val card = intent.getStringExtra(EXTRA_CARD).orEmpty()
        addressValue.text = address.ifBlank { getString(R.string.placeholder_dash) }
        cardValue.text = card.ifBlank { getString(R.string.placeholder_dash) }

        val imagePath = receipt.imagePath
        if (imagePath.isNotBlank() && File(imagePath).exists()) {
            receiptImage.setImageURI(Uri.fromFile(File(imagePath)))
        } else {
            receiptImage.setImageResource(R.drawable.ic_receipts)
        }
    }

    private fun openEditDialog() {
        val receipt = currentReceipt ?: return
        EditReceiptDialog(receipt) { updated ->
            currentReceipt = updated
            bindReceipt(updated)
            viewModel.updateReceipt(updated)
            Toast.makeText(this, getString(R.string.success), Toast.LENGTH_SHORT).show()
        }.show(supportFragmentManager, "edit_receipt_details")
    }

    private fun formatDisplayDate(timestamp: Long): String {
        val format = SimpleDateFormat("yyyy/MM/dd hh:mma", Locale.getDefault())
        return format.format(Date(timestamp))
    }
}
