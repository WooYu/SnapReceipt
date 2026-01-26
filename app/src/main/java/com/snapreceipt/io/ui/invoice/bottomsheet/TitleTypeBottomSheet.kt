package com.snapreceipt.io.ui.invoice.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R

class TitleTypeBottomSheet(
    private val initialSelection: String?,
    private val onSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var optionViews: List<TextView>
    private var selectedLabel: String = initialSelection ?: ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_title_type, null)
        dialog.setContentView(view)

        optionViews = listOf(
            view.findViewById(R.id.title_individual),
            view.findViewById(R.id.title_company)
        )

        optionViews.forEach { option ->
            option.setOnClickListener {
                selectedLabel = option.text.toString()
                updateSelection()
            }
        }

        view.findViewById<View>(R.id.cancel_btn).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.confirm_btn).setOnClickListener {
            onSelected(selectedLabel)
            dismiss()
        }

        updateSelection()
        return dialog
    }

    private fun updateSelection() {
        if (optionViews.none { it.text.toString().equals(selectedLabel, ignoreCase = true) }) {
            selectedLabel = optionViews.firstOrNull()?.text?.toString().orEmpty()
        }
        optionViews.forEach { option ->
            option.isSelected = option.text.toString().equals(selectedLabel, ignoreCase = true)
        }
    }
}
