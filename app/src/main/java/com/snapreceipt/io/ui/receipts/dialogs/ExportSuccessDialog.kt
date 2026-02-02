package com.snapreceipt.io.ui.receipts.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.snapreceipt.io.databinding.DialogExportSuccessBinding

class ExportSuccessDialog(
    private val onViewRecords: () -> Unit
) : DialogFragment() {

    private var _binding: DialogExportSuccessBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        _binding = DialogExportSuccessBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        binding.closeBtn.setOnClickListener { dismiss() }
        binding.viewRecordsBtn.setOnClickListener {
            onViewRecords()
            dismiss()
        }
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
