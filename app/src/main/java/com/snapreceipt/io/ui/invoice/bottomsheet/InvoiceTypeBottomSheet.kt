package com.snapreceipt.io.ui.invoice.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.usecase.category.AddCategoryUseCase
import com.snapreceipt.io.domain.usecase.category.DeleteCategoryUseCase
import com.snapreceipt.io.domain.usecase.category.FetchCategoriesUseCase
import com.snapreceipt.io.ui.invoice.dialogs.CustomTypeDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class InvoiceTypeBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_INITIAL = "arg_initial"

        fun newInstance(initialSelection: String?, onSelected: (String) -> Unit): InvoiceTypeBottomSheet {
            return InvoiceTypeBottomSheet().apply {
                arguments = bundleOf(ARG_INITIAL to initialSelection)
                this.onSelected = onSelected
            }
        }
    }

    @Inject
    lateinit var fetchCategoriesUseCase: FetchCategoriesUseCase

    @Inject
    lateinit var addCategoryUseCase: AddCategoryUseCase

    @Inject
    lateinit var deleteCategoryUseCase: DeleteCategoryUseCase

    private var onSelected: ((String) -> Unit)? = null
    private lateinit var adapter: CategoryChipAdapter
    private var selectedLabel: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_invoice_type, null)
        dialog.setContentView(view)

        selectedLabel = arguments?.getString(ARG_INITIAL).orEmpty()

        val recycler = view.findViewById<RecyclerView>(R.id.category_list)
        adapter = CategoryChipAdapter(
            onSelect = { option ->
                selectedLabel = option.label
                adapter.updateSelection(selectedLabel)
            },
            onLongPress = { option ->
                if (option.isCustom && !option.isAll) {
                    confirmDelete(option)
                }
            }
        )
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        recycler.adapter = adapter

        view.findViewById<TextView>(R.id.type_add).setOnClickListener {
            CustomTypeDialog { customType -> addCustomType(customType) }
                .show(parentFragmentManager, "custom_type_dialog")
        }

        view.findViewById<View>(R.id.cancel_btn).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.confirm_btn).setOnClickListener {
            onSelected?.invoke(selectedLabel)
            dismiss()
        }

        loadCategories()
        return dialog
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { fetchCategoriesUseCase() }
            val list = result.getOrElse { ReceiptCategory.all() }
            ReceiptCategory.update(list)
            val options = buildOptions(list)
            adapter.submitList(options, selectedLabel)
        }
    }

    private fun addCustomType(label: String) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { addCategoryUseCase(label) }
            result.onSuccess {
                selectedLabel = label
                loadCategories()
            }.onFailure {
                Toast.makeText(requireContext(), getString(R.string.add_category_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(option: CategoryOption) {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.delete_category_confirm, option.label))
            .setPositiveButton(R.string.confirm) { _, _ -> deleteCategory(option) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteCategory(option: CategoryOption) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { deleteCategoryUseCase(listOf(option.id)) }
            result.onSuccess {
                if (selectedLabel.equals(option.label, ignoreCase = true)) {
                    selectedLabel = getString(R.string.type_all)
                }
                loadCategories()
            }.onFailure {
                Toast.makeText(requireContext(), getString(R.string.delete_category_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildOptions(list: List<ReceiptCategory.Item>): List<CategoryOption> {
        val options = mutableListOf<CategoryOption>()
        options.add(CategoryOption(0, getString(R.string.type_all), isCustom = false, isAll = true))
        list.forEach { item ->
            options.add(CategoryOption(item.id, item.label, item.isCustom, isAll = false))
        }
        if (selectedLabel.isBlank() || options.none { it.label.equals(selectedLabel, ignoreCase = true) }) {
            selectedLabel = options.first().label
        }
        return options
    }

    data class CategoryOption(
        val id: Int,
        val label: String,
        val isCustom: Boolean,
        val isAll: Boolean
    )

    class CategoryChipAdapter(
        private val onSelect: (CategoryOption) -> Unit,
        private val onLongPress: (CategoryOption) -> Unit
    ) : RecyclerView.Adapter<CategoryChipAdapter.ViewHolder>() {

        private var items: List<CategoryOption> = emptyList()
        private var selectedLabel: String = ""

        fun submitList(list: List<CategoryOption>, selected: String) {
            items = list
            selectedLabel = selected
            notifyDataSetChanged()
        }

        fun updateSelection(label: String) {
            selectedLabel = label
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_chip, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], selectedLabel, onSelect, onLongPress)
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val text: TextView = itemView.findViewById(R.id.chip_text)

            fun bind(
                option: CategoryOption,
                selectedLabel: String,
                onSelect: (CategoryOption) -> Unit,
                onLongPress: (CategoryOption) -> Unit
            ) {
                text.text = option.label
                text.isSelected = option.label.equals(selectedLabel, ignoreCase = true)
                text.setOnClickListener { onSelect(option) }
                text.setOnLongClickListener {
                    if (option.isCustom && !option.isAll) {
                        onLongPress(option)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }
}
