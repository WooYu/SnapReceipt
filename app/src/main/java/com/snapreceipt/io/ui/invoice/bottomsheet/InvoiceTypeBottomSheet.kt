package com.snapreceipt.io.ui.invoice.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.ui.invoice.dialogs.CustomTypeDialog

class InvoiceTypeBottomSheet(
    private val initialSelection: String?,
    private val onSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    companion object {
        private val savedCustomTypes = mutableListOf<String>()
    }

    private lateinit var optionViews: MutableList<TextView>
    private lateinit var customTypeContainer: LinearLayout
    private var selectedLabel: String = initialSelection ?: ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_invoice_type, null)
        dialog.setContentView(view)

        optionViews = mutableListOf(
            view.findViewById(R.id.type_all),
            view.findViewById(R.id.type_food),
            view.findViewById(R.id.type_travel),
            view.findViewById(R.id.type_office),
            view.findViewById(R.id.type_hotel),
            view.findViewById(R.id.type_other)
        )
        customTypeContainer = view.findViewById(R.id.custom_type_container)

        optionViews.forEach { option -> bindOption(option) }

        view.findViewById<TextView>(R.id.type_add).setOnClickListener {
            CustomTypeDialog { customType ->
                addCustomType(customType)
                selectedLabel = customType
                updateSelection()
            }.show(parentFragmentManager, "custom_type_dialog")
        }

        view.findViewById<View>(R.id.cancel_btn).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.confirm_btn).setOnClickListener {
            onSelected(selectedLabel)
            dismiss()
        }

        savedCustomTypes.forEach { addCustomType(it, persist = false) }
        if (selectedLabel.isNotBlank()) {
            addCustomType(selectedLabel)
        }
        updateSelection()
        return dialog
    }

    private fun bindOption(option: TextView) {
        option.setOnClickListener {
            selectedLabel = option.text.toString()
            updateSelection()
        }
    }

    private fun addCustomType(label: String, persist: Boolean = true) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return
        if (optionViews.any { it.text.toString().equals(trimmed, ignoreCase = true) }) return
        if (persist) {
            savedCustomTypes.add(trimmed)
        }
        val chip = TextView(requireContext()).apply {
            text = trimmed
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_selector)
            setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector))
        }
        bindOption(chip)
        optionViews.add(chip)
        addChipToRow(chip)
    }

    private fun addChipToRow(chip: TextView) {
        val row = findOrCreateRow()
        val margin = if (row.childCount > 0) dpToPx(12) else 0
        val params = LinearLayout.LayoutParams(0, dpToPx(44), 1f).apply {
            marginStart = margin
        }
        row.addView(chip, params)
    }

    private fun findOrCreateRow(): LinearLayout {
        if (customTypeContainer.childCount > 0) {
            val last = customTypeContainer.getChildAt(customTypeContainer.childCount - 1) as LinearLayout
            if (last.childCount < 3) return last
        }
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (customTypeContainer.childCount > 0) {
                    topMargin = dpToPx(12)
                }
            }
        }
        customTypeContainer.addView(row)
        return row
    }

    private fun dpToPx(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun updateSelection() {
        if (optionViews.none { it.text.toString().equals(selectedLabel, ignoreCase = true) }) {
            selectedLabel = optionViews.firstOrNull()?.text?.toString().orEmpty()
        }
        optionViews.forEach { option ->
            option.isSelected = option.text.toString().equals(selectedLabel, ignoreCase = true)
        }
    }
}
