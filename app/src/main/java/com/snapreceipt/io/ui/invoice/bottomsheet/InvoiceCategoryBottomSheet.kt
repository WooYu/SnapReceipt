package com.snapreceipt.io.ui.invoice.bottomsheet

import android.animation.ObjectAnimator
import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetInvoiceCategoryBinding
import com.snapreceipt.io.databinding.ItemCategoryChipBinding
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
        private const val GRID_SPAN_COUNT = 3
        private const val CATEGORY_ROW_HEIGHT_DP = 62f
        private const val CATEGORY_MIN_HEIGHT_DP = 120f
        private const val CATEGORY_MAX_HEIGHT_RATIO = 0.45f
        private const val CATEGORY_ITEM_SPACING_HORIZONTAL_DP = 10f
        private const val CATEGORY_ITEM_SPACING_VERTICAL_DP = 12f

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

    private var _binding: BottomSheetInvoiceCategoryBinding? = null
    private val binding get() = _binding!!

    private var onSelected: ((String) -> Unit)? = null
    private lateinit var adapter: CategoryChipAdapter
    private var selectedLabel: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        _binding = BottomSheetInvoiceCategoryBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(binding.root)
        applyBottomInsets()
        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = false
            }
        }

        selectedLabel = arguments?.getString(ARG_INITIAL).orEmpty()

        adapter = CategoryChipAdapter(
            onSelect = { option ->
                selectedLabel = if (option.label.equals(selectedLabel, ignoreCase = true)) {
                    ""
                } else {
                    option.label
                }
                adapter.updateSelection(selectedLabel)
            },
            onLongPress = { option ->
                if (option.isCustom && !option.isAll) {
                    confirmDelete(option)
                }
            }
        )
        binding.categoryList.layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
        binding.categoryList.adapter = adapter
        binding.categoryList.setHasFixedSize(true)
        binding.categoryList.isNestedScrollingEnabled = true
        binding.categoryList.overScrollMode = View.OVER_SCROLL_NEVER
        if (binding.categoryList.itemDecorationCount == 0) {
            binding.categoryList.addItemDecoration(
                GridSpacingItemDecoration(
                    spanCount = GRID_SPAN_COUNT,
                    horizontalSpacing = dpToPx(CATEGORY_ITEM_SPACING_HORIZONTAL_DP),
                    verticalSpacing = dpToPx(CATEGORY_ITEM_SPACING_VERTICAL_DP)
                )
            )
        }
        binding.categoryList.setOnTouchListener { view, _ ->
            view.parent?.requestDisallowInterceptTouchEvent(true)
            false
        }

        binding.typeAdd.setOnClickListener {
            CustomTypeDialog { customType -> addCustomType(customType) }
                .show(parentFragmentManager, "custom_type_dialog")
        }

        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.confirmBtn.setOnClickListener {
            onSelected?.invoke(selectedLabel)
            dismiss()
        }

        val cached = ReceiptCategory.all()
        if (cached.isNotEmpty()) {
            applyCategories(cached)
        }
        refreshCategoriesFromRemote()
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
        super.onDestroyView()
        _binding = null
    }

    private fun refreshCategoriesFromRemote() {
        lifecycleScope.launch {
            val result = withContext(dispatchers.io) { fetchCategoriesUseCase() }
            result.onSuccess { list ->
                ReceiptCategory.update(list)
                applyCategories(list)
            }.onFailure {
                if (adapter.itemCount == 0) {
                    val cached = ReceiptCategory.all()
                    if (cached.isNotEmpty()) {
                        applyCategories(cached)
                    }
                }
            }
        }
    }

    private fun addCustomType(label: String) {
        lifecycleScope.launch {
            val result = withContext(dispatchers.io) { addCategoryUseCase(label) }
            result.onSuccess {
                selectedLabel = label
                refreshCategoriesFromRemote()
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
                refreshCategoriesFromRemote()
            }.onFailure {
                Toast.makeText(requireContext(), getString(R.string.delete_category_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyCategories(list: List<ReceiptCategory.Item>) {
        val options = buildOptions(list)
        adapter.submitList(options, selectedLabel)
        binding.deleteHint.isVisible = options.any { it.isCustom && !it.isAll }
        updateCategoryListHeight(options.size)
    }

    private fun updateCategoryListHeight(itemCount: Int) {
        val rows = maxOf(1, (itemCount + GRID_SPAN_COUNT - 1) / GRID_SPAN_COUNT)
        val rowHeight = (resources.displayMetrics.density * CATEGORY_ROW_HEIGHT_DP).toInt()
        val desiredHeight = rows * rowHeight
        val minHeight = (resources.displayMetrics.density * CATEGORY_MIN_HEIGHT_DP).toInt()
        val maxHeight = (resources.displayMetrics.heightPixels * CATEGORY_MAX_HEIGHT_RATIO).toInt()
        val finalHeight = desiredHeight.coerceIn(minHeight, maxHeight)
        binding.categoryList.updateLayoutParams<ViewGroup.LayoutParams> {
            height = finalHeight
        }
    }

    private fun buildOptions(list: List<ReceiptCategory.Item>): List<CategoryOption> {
        val options = mutableListOf<CategoryOption>()
        list.forEach { item ->
            options.add(CategoryOption(item.id, item.label, item.isCustom, isAll = false))
        }
        if (selectedLabel.isNotBlank() && options.none { it.label.equals(selectedLabel, ignoreCase = true) }) {
            selectedLabel = ""
        }
        return options
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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
                        binding.chipText.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        playDeleteAnimation(binding.chipText)
                        onLongPress(option)
                        true
                    } else {
                        false
                    }
                }
            }

            private fun playDeleteAnimation(target: View) {
                ObjectAnimator.ofFloat(target, View.ROTATION, 0f, -2.5f, 2.5f, -2f, 2f, 0f).apply {
                    duration = 260L
                    start()
                }
            }
        }
    }

    private class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val horizontalSpacing: Int,
        private val verticalSpacing: Int
    ) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            val column = position % spanCount

            outRect.left = horizontalSpacing - column * horizontalSpacing / spanCount
            outRect.right = (column + 1) * horizontalSpacing / spanCount
            if (position >= spanCount) {
                outRect.top = verticalSpacing
            }
        }
    }
}
