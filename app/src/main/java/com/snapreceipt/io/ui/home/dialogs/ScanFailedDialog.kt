package com.snapreceipt.io.ui.home.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.snapreceipt.io.databinding.DialogScanFailedBinding

class ScanFailedDialog : DialogFragment() {
    private var _binding: DialogScanFailedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        return activity?.let {
            _binding = DialogScanFailedBinding.inflate(LayoutInflater.from(requireContext()))
            binding.closeBtn.setOnClickListener { dismiss() }
            binding.returnBtn.setOnClickListener { dismiss() }
            AlertDialog.Builder(requireContext())
                .setView(binding.root)
                .create()
        } ?: super.onCreateDialog(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onDestroyView() {
        _binding?.closeBtn?.setOnClickListener(null)
        _binding?.returnBtn?.setOnClickListener(null)
        _binding = null
        super.onDestroyView()
    }
}
