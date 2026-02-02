package com.snapreceipt.io.ui.invoice.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.DialogCustomTypeBinding

class CustomTypeDialog(
    private val onConfirm: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogCustomTypeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        _binding = DialogCustomTypeBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.confirmBtn.setOnClickListener {
            val value = binding.customTypeInput.text.toString().trim()
            if (value.isBlank()) {
                Toast.makeText(requireContext(), getString(R.string.custom_type_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onConfirm(value)
            dismiss()
        }

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
