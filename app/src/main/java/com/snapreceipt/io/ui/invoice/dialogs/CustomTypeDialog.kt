package com.snapreceipt.io.ui.invoice.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.DialogCustomTypeBinding

class CustomTypeDialog(
    private val onConfirm: (String, (Boolean) -> Unit) -> Unit
) : DialogFragment() {

    private var _binding: DialogCustomTypeBinding? = null
    private val binding get() = _binding!!
    private var isSubmitting = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        _binding = DialogCustomTypeBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.cancelBtn.setOnClickListener {
            if (!isSubmitting) dismiss()
        }
        binding.confirmBtn.setOnClickListener {
            if (isSubmitting) return@setOnClickListener

            val value = binding.customTypeInput.text.toString().trim()
            if (value.isBlank()) {
                Toast.makeText(requireContext(), getString(R.string.custom_type_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setSubmitting(true)
            onConfirm(value) { success ->
                if (_binding != null) {
                    if (success) {
                        dismissAllowingStateLoss()
                    } else {
                        setSubmitting(false)
                    }
                }
            }
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        val width = (resources.displayMetrics.widthPixels * 0.9f).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        dialog?.setCanceledOnTouchOutside(!isSubmitting)
    }

    override fun onDestroyView() {
        _binding?.cancelBtn?.setOnClickListener(null)
        _binding?.confirmBtn?.setOnClickListener(null)
        isSubmitting = false
        _binding = null
        super.onDestroyView()
    }

    private fun setSubmitting(submitting: Boolean) {
        isSubmitting = submitting

        val currentBinding = _binding ?: return
        currentBinding.loadingContainer.isVisible = submitting
        currentBinding.customTypeInput.isEnabled = !submitting
        currentBinding.cancelBtn.isEnabled = !submitting
        currentBinding.confirmBtn.isEnabled = !submitting
        isCancelable = !submitting
        dialog?.setCanceledOnTouchOutside(!submitting)
    }
}
