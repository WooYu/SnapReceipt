package com.snapreceipt.io.ui.receipts.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.snapreceipt.io.R

class ExportSuccessDialog(
    private val onViewRecords: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_export_success, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        view.findViewById<View>(R.id.close_btn).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.view_records_btn).setOnClickListener {
            onViewRecords()
            dismiss()
        }
        return dialog
    }
}
