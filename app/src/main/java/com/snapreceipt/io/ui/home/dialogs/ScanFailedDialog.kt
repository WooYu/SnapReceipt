package com.snapreceipt.io.ui.home.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.snapreceipt.io.R

class ScanFailedDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_scan_failed, null)
            view.findViewById<ImageView>(R.id.close_btn).setOnClickListener { dismiss() }
            view.findViewById<Button>(R.id.return_btn).setOnClickListener { dismiss() }
            AlertDialog.Builder(requireContext())
                .setView(view)
                .create()
        } ?: super.onCreateDialog(savedInstanceState)
    }
}
