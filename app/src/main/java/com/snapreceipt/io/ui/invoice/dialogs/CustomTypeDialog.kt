package com.snapreceipt.io.ui.invoice.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R

class CustomTypeDialog(
    private val onConfirm: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_custom_type, null)
        dialog.setContentView(view)

        val input = view.findViewById<EditText>(R.id.custom_type_input)

        view.findViewById<View>(R.id.cancel_btn).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.confirm_btn).setOnClickListener {
            val value = input.text.toString().trim()
            if (value.isBlank()) {
                Toast.makeText(requireContext(), getString(R.string.custom_type_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onConfirm(value)
            dismiss()
        }

        return dialog
    }
}
