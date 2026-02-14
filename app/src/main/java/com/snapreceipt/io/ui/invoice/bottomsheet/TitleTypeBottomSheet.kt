package com.snapreceipt.io.ui.invoice.bottomsheet

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetTitleTypeBinding

class TitleTypeBottomSheet(
    private val initialSelection: String?,
    private val onSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTitleTypeBinding? = null
    private val binding get() = _binding!!
    private var selectedLabel: String = initialSelection?.trim().orEmpty()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        _binding = BottomSheetTitleTypeBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }
        applyBottomInsets()

        val optionViews = listOf(binding.titleIndividual, binding.titleCompany)
        optionViews.forEach { option ->
            option.setOnClickListener {
                val label = option.text.toString().trim()
                selectedLabel = if (label.equals(selectedLabel, ignoreCase = true)) {
                    ""
                } else {
                    label
                }
                updateSelection(optionViews)
            }
        }

        binding.cancelBtn.text = getString(R.string.reset)
        binding.cancelBtn.setOnClickListener {
            selectedLabel = ""
            onSelected(selectedLabel)
            dismiss()
        }
        binding.confirmBtn.setOnClickListener {
            onSelected(selectedLabel)
            dismiss()
        }

        updateSelection(optionViews)
        return dialog
    }

    private fun applyBottomInsets() {
        val initialBottomPadding = binding.rootContainer.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootContainer) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updatePadding(bottom = initialBottomPadding + bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.rootContainer)
    }

    override fun onDestroyView() {
        _binding?.cancelBtn?.setOnClickListener(null)
        _binding?.confirmBtn?.setOnClickListener(null)
        _binding?.titleIndividual?.setOnClickListener(null)
        _binding?.titleCompany?.setOnClickListener(null)
        _binding?.rootContainer?.let { ViewCompat.setOnApplyWindowInsetsListener(it, null) }
        _binding = null
        super.onDestroyView()
    }

    private fun updateSelection(optionViews: List<android.widget.TextView>) {
        optionViews.forEach { option ->
            option.isSelected = option.text.toString().trim()
                .equals(selectedLabel, ignoreCase = true)
        }
    }
}
