package com.snapreceipt.io.ui.invoice.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetTitleTypeBinding
import com.snapreceipt.io.ui.widget.bottomsheet.applyHorizontalMargins

class TitleTypeBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_INITIAL = "arg_initial"
        private const val STATE_SELECTED_LABEL = "state_selected_label"

        fun newInstance(
            initialSelection: String?,
            onSelected: (String) -> Unit
        ): TitleTypeBottomSheet {
            return TitleTypeBottomSheet().apply {
                arguments = bundleOf(ARG_INITIAL to initialSelection)
                this.onSelected = onSelected
            }
        }
    }

    private var _binding: BottomSheetTitleTypeBinding? = null
    private val binding get() = _binding!!
    private var selectedLabel: String = ""
    private var onSelected: ((String) -> Unit)? = null

    private lateinit var options: List<OptionRow>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedLabel =
            savedInstanceState?.getString(STATE_SELECTED_LABEL)
                ?: arguments?.getString(ARG_INITIAL).orEmpty()
        selectedLabel = selectedLabel.trim()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        _binding = BottomSheetTitleTypeBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.applyHorizontalMargins(resources.getDimensionPixelSize(R.dimen.page_start_margin))
        applyBottomInsets()

        options = listOf(
            OptionRow(binding.optionIndividual, binding.titleIndividual, binding.checkIndividual),
            OptionRow(binding.optionBusiness, binding.titleCompany, binding.checkBusiness)
        )

        options.forEach { option ->
            option.container.setOnClickListener {
                val label = option.text.text.toString().trim()
                selectedLabel = if (label.equals(selectedLabel, ignoreCase = true)) "" else label
                updateSelection()
            }
        }

        binding.cancelBtn.setOnClickListener {
            selectedLabel = ""
            updateSelection()
        }
        binding.confirmBtn.setOnClickListener {
            onSelected?.invoke(selectedLabel)
            dismiss()
        }

        updateSelection()
        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SELECTED_LABEL, selectedLabel)
        super.onSaveInstanceState(outState)
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
        _binding?.optionIndividual?.setOnClickListener(null)
        _binding?.optionBusiness?.setOnClickListener(null)
        _binding?.rootContainer?.let { ViewCompat.setOnApplyWindowInsetsListener(it, null) }
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        onSelected = null
        super.onDestroy()
    }

    private fun updateSelection() {
        if (!::options.isInitialized) return
        options.forEach { option ->
            val isSelected = option.text.text.toString().trim()
                .equals(selectedLabel, ignoreCase = true)

            if (isSelected) {
                option.container.setBackgroundResource(R.drawable.bg_chip_selected)
                option.text.setTextColor(
                    resources.getColor(R.color.colorPrimary, requireContext().theme)
                )
                option.check.isVisible = true
            } else {
                option.container.setBackgroundResource(R.drawable.bg_surface_card_small)
                option.text.setTextColor(
                    resources.getColor(R.color.text_primary, requireContext().theme)
                )
                option.check.isVisible = false
            }
        }
    }

    private data class OptionRow(
        val container: ViewGroup,
        val text: TextView,
        val check: ImageView
    )
}
