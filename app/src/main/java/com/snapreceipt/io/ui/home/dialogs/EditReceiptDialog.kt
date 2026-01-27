package com.snapreceipt.io.ui.home.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptEntity

class EditReceiptDialog(
    private val receipt: ReceiptEntity,
    private val onSave: (ReceiptEntity) -> Unit
) : DialogFragment() {

    private lateinit var merchantNameInput: TextInputEditText
    private lateinit var amountInput: TextInputEditText
    private lateinit var categoryInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_receipt, null)
            
            merchantNameInput = view.findViewById(R.id.merchant_name_input)
            amountInput = view.findViewById(R.id.amount_input)
            categoryInput = view.findViewById(R.id.category_input)
            descriptionInput = view.findViewById(R.id.description_input)

            // Set current values
            merchantNameInput.setText(receipt.merchant.orEmpty())
            amountInput.setText(receipt.totalAmount?.toString().orEmpty())
            val categoryLabel = receipt.categoryId?.let { ReceiptCategory.labelForId(it) }.orEmpty()
            categoryInput.setText(categoryLabel)
            descriptionInput.setText(receipt.remark.orEmpty())

            val cancelBtn = view.findViewById<Button>(R.id.cancel_btn)
            val saveBtn = view.findViewById<Button>(R.id.save_btn)

            cancelBtn.setOnClickListener { dismiss() }
            saveBtn.setOnClickListener { onSaveClick() }

            AlertDialog.Builder(requireContext())
                .setView(view)
                .create()
        } ?: super.onCreateDialog(savedInstanceState)
    }

    private fun onSaveClick() {
        val merchantName = merchantNameInput.text.toString().trim()
        val amount = amountInput.text.toString().trim().toDoubleOrNull() ?: receipt.totalAmount
        val category = categoryInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()

        val updated = receipt.copy(
            merchant = merchantName.ifEmpty { receipt.merchant.orEmpty() },
            totalAmount = amount,
            categoryId = ReceiptCategory.idForLabel(category).takeIf { it > 0L } ?: receipt.categoryId,
            remark = description
        )

        onSave(updated)
        dismiss()
    }
}
