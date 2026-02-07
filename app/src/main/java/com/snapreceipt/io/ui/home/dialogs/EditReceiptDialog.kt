package com.snapreceipt.io.ui.home.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.snapreceipt.io.databinding.DialogEditReceiptBinding
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptEntity

class EditReceiptDialog(
    private val receipt: ReceiptEntity,
    private val onSave: (ReceiptEntity) -> Unit
) : DialogFragment() {

    private var _binding: DialogEditReceiptBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        return activity?.let {
            _binding = DialogEditReceiptBinding.inflate(LayoutInflater.from(requireContext()))

            binding.merchantNameInput.setText(receipt.merchant.orEmpty())
            binding.amountInput.setText(receipt.totalAmount?.toString().orEmpty())
            binding.categoryInput.setText(receipt.categoryName.orEmpty())
            binding.descriptionInput.setText(receipt.remark.orEmpty())

            binding.cancelBtn.setOnClickListener { dismiss() }
            binding.saveBtn.setOnClickListener { onSaveClick() }

            AlertDialog.Builder(requireContext())
                .setView(binding.root)
                .create()
        } ?: super.onCreateDialog(savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onSaveClick() {
        val merchantName = binding.merchantNameInput.text.toString().trim()
        val amount = binding.amountInput.text.toString().trim().toDoubleOrNull() ?: receipt.totalAmount
        val category = binding.categoryInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()

        val newCategoryId = ReceiptCategory.idForLabel(category).takeIf { it > 0L } ?: receipt.categoryId
        val updated = receipt.copy(
            merchant = merchantName.ifEmpty { receipt.merchant.orEmpty() },
            totalAmount = amount,
            categoryId = newCategoryId,
            categoryName = category.ifEmpty { receipt.categoryName },
            remark = description
        )

        onSave(updated)
        dismiss()
    }
}
