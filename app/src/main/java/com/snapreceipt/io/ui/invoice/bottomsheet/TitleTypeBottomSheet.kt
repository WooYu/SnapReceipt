package com.snapreceipt.io.ui.invoice.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.databinding.BottomSheetTitleTypeBinding

class TitleTypeBottomSheet(
    private val initialSelection: String?,
    private val onSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTitleTypeBinding? = null
    private val binding get() = _binding!!
    private var selectedLabel: String = initialSelection ?: ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        _binding = BottomSheetTitleTypeBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        val optionViews = listOf(binding.titleIndividual, binding.titleCompany)
        optionViews.forEach { option ->
            option.setOnClickListener {
                selectedLabel = option.text.toString()
                updateSelection(optionViews)
            }
        }

        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.confirmBtn.setOnClickListener {
            onSelected(selectedLabel)
            dismiss()
        }

        updateSelection(optionViews)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateSelection(optionViews: List<android.widget.TextView>) {
        if (optionViews.none { it.text.toString().equals(selectedLabel, ignoreCase = true) }) {
            selectedLabel = optionViews.firstOrNull()?.text?.toString().orEmpty()
        }
        optionViews.forEach { option ->
            option.isSelected = option.text.toString().equals(selectedLabel, ignoreCase = true)
        }
    }
}
