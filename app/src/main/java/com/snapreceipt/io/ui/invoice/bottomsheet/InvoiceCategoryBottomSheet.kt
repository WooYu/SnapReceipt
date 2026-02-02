package com.snapreceipt.io.ui.invoice.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetInvoiceCategoryBinding
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.usecase.category.AddCategoryUseCase
import com.snapreceipt.io.domain.usecase.category.DeleteCategoryUseCase
import com.snapreceipt.io.domain.usecase.category.FetchCategoriesUseCase
import com.snapreceipt.io.ui.invoice.dialogs.CustomTypeDialog
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class InvoiceCategoryBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_INITIAL = "arg_initial"

        fun newInstance(initialSelection: String?, onSelected: (String) -> Unit): InvoiceCategoryBottomSheet {
            return InvoiceCategoryBottomSheet().apply {
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

    @Inject
    lateinit var dispatchers: CoroutineDispatchersProvider

    private var onSelected: ((String) -> Unit)? = null
    private lateinit var adapter: CategoryChipAdapter
    private var selectedLabel: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val binding = BottomSheetInvoiceCategoryBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(binding.root)

        selectedLabel = arguments?.getString(ARG_INITIAL).orEmpty()

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
        binding.categoryList.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.categoryList.adapter = adapter

        binding.typeAdd.setOnClickListener {
            CustomTypeDialog { customType -> addCustomType(customType) }
                .show(parentFragmentManager, "custom_type_dialog")
        }

        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.confirmBtn.setOnClickListener {
            onSelected?.invoke(selectedLabel)
            dismiss()
        }

        loadCategories()
        return dialog
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val result = withContext(dispatchers.io) { fetchCategoriesUseCase() }
            val list = result.getOrElse { ReceiptCategory.all() }
            ReceiptCategory.update(list)
            val options = buildOptions(list)
            adapter.submitList(options, selectedLabel)
        }
    }

    private fun addCustomType(label: String) {
        lifecycleScope.launch {
            val result = withContext(dispatchers.io) { addCategoryUseCase(label) }
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
            val result = withContext(dispatchers.io) { deleteCategoryUseCase(listOf(option.id)) }
            result.onSuccess {
                if (selectedLabel.equals(option.label, ignoreCase = true)) {
                    selectedLabel = ""
                }
                loadCategories()
            }.onFailure {
                Toast.makeText(requireContext(), getString(R.string.delete_category_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildOptions(list: List<ReceiptCategory.Item>): List<CategoryOption> {
        val options = mutableListOf<CategoryOption>()
        list.forEach { item ->
            options.add(CategoryOption(item.id, item.label, item.isCustom, isAll = false))
        }
        if (options.isEmpty()) {
            selectedLabel = ""
            return options
        }
        if (selectedLabel.isBlank() || options.none { it.label.equals(selectedLabel, ignoreCase = true) }) {
            selectedLabel = options.first().label
        }
        return options
    }

    data class CategoryOption(
        val id: Long,
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
            val binding = ItemCategoryChipBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], selectedLabel, onSelect, onLongPress)
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(
            private val binding: ItemCategoryChipBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(
                option: CategoryOption,
                selectedLabel: String,
                onSelect: (CategoryOption) -> Unit,
                onLongPress: (CategoryOption) -> Unit
            ) {
                binding.chipText.text = option.label
                binding.chipText.isSelected = option.label.equals(selectedLabel, ignoreCase = true)
                binding.chipText.setOnClickListener { onSelect(option) }
                binding.chipText.setOnLongClickListener {
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
