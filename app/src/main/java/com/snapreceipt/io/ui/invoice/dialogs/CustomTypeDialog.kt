package com.snapreceipt.io.ui.invoice.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.DialogCustomTypeBinding

class CustomTypeDialog(
    private val onConfirm: (String) -> Unit
) : DialogFragment() {

    private var _binding: DialogCustomTypeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        _binding = DialogCustomTypeBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

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

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        val width = (resources.displayMetrics.widthPixels * 0.9f).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
    }

    override fun onDestroyView() {
        _binding?.cancelBtn?.setOnClickListener(null)
        _binding?.confirmBtn?.setOnClickListener(null)
        _binding = null
        super.onDestroyView()
    }
}
