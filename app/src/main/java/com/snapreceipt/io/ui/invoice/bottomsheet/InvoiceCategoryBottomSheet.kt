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
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetInvoiceCategoryBinding
import com.snapreceipt.io.databinding.ItemCategoryAddBinding
import com.snapreceipt.io.databinding.ItemCategoryChipBinding
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.usecase.category.AddCategoryUseCase
import com.snapreceipt.io.domain.usecase.category.DeleteCategoryUseCase
import com.snapreceipt.io.domain.usecase.category.FetchCategoriesUseCase
import com.snapreceipt.io.ui.invoice.dialogs.CustomTypeDialog
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class InvoiceCategoryBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_INITIAL = "arg_initial"
        private const val STATE_SELECTED_LABEL = "state_selected_label"

        // 3 columns to balance readability and touch area in bottom sheet width.
        private const val GRID_SPAN_COUNT = 3
        private const val CATEGORY_FALLBACK_ROW_HEIGHT_DP = 62f
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
    private val binding: BottomSheetInvoiceCategoryBinding
        get() = checkNotNull(_binding) {
            "Binding is only valid between onCreateView and onDestroyView"
        }

    private var onSelected: ((String) -> Unit)? = null
    private val adapter by lazy {
        CategoryChipAdapter(
            onSelect = { option -> toggleSelection(option.label) },
            onLongPress = { option ->
                if (option.isCustom) {
                    confirmDelete(option)
                }
            },
            onAddClick = ::openAddCategoryDialog
        )
    }

    private var selectedLabel: String = ""
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedLabel =
            savedInstanceState?.getString(STATE_SELECTED_LABEL)
                ?: arguments?.getString(ARG_INITIAL).orEmpty()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext()).also(::setupBottomSheet)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetInvoiceCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyBottomInsets()
        setupCategoryList()
        setupActions()

        // Show cached categories immediately so UI is not blocked by network.
        // Even when cache is empty, render the trailing "Add" action as list item.
        applyCategories(ReceiptCategory.all())
        // Then refresh from remote and reconcile selection state.
        refreshCategoriesFromRemote()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SELECTED_LABEL, selectedLabel)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        refreshJob?.cancel()
        refreshJob = null
        _binding?.categoryList?.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        onSelected = null
        super.onDestroy()
    }

    private fun setupBottomSheet(dialog: BottomSheetDialog) {
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
    }

    private fun setupCategoryList() {
        binding.categoryList.apply {
            layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
            adapter = this@InvoiceCategoryBottomSheet.adapter
            setHasFixedSize(true)
            isNestedScrollingEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
            if (itemDecorationCount == 0) {
                // ItemDecoration provides spacing around chips (visual divider/gutter effect).
                addItemDecoration(
                    GridSpacingItemDecoration(
                        spanCount = GRID_SPAN_COUNT,
                        horizontalSpacing = dpToPx(CATEGORY_ITEM_SPACING_HORIZONTAL_DP),
                        verticalSpacing = dpToPx(CATEGORY_ITEM_SPACING_VERTICAL_DP)
                    )
                )
            }
            setOnTouchListener { touchedView, _ ->
                touchedView.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    private fun setupActions() {
        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.confirmBtn.setOnClickListener {
            onSelected?.invoke(selectedLabel)
            dismiss()
        }
    }

    private fun openAddCategoryDialog() {
        CustomTypeDialog { customType -> addCustomType(customType) }
            .show(parentFragmentManager, "custom_type_dialog")
    }

    private fun toggleSelection(label: String) {
        selectedLabel = if (label.equals(selectedLabel, ignoreCase = true)) {
            ""
        } else {
            label
        }
        adapter.updateSelection(selectedLabel)
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

    private fun refreshCategoriesFromRemote() {
        refreshJob?.cancel()
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(dispatchers.io) { fetchCategoriesUseCase() }
            result.onSuccess { list ->
                ReceiptCategory.update(list)
                applyCategories(list)
            }.onFailure {
                // If request fails and nothing is rendered yet, fallback to cached data.
                if (!adapter.hasCategoryItems()) {
                    val cached = ReceiptCategory.all()
                    if (cached.isNotEmpty()) {
                        applyCategories(cached)
                    }
                }
            }
        }
    }

    private fun addCustomType(rawLabel: String) {
        val sanitized = rawLabel.trim()
        if (sanitized.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(dispatchers.io) { addCategoryUseCase(sanitized) }
            result.onSuccess {
                selectedLabel = sanitized
                refreshCategoriesFromRemote()
            }.onFailure {
                showToast(R.string.add_category_failed)
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
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(dispatchers.io) { deleteCategoryUseCase(listOf(option.id)) }
            result.onSuccess {
                if (selectedLabel.equals(option.label, ignoreCase = true)) {
                    selectedLabel = ""
                }
                refreshCategoriesFromRemote()
            }.onFailure {
                showToast(R.string.delete_category_failed)
            }
        }
    }

    private fun applyCategories(list: List<ReceiptCategory.Item>) {
        val options = list.map { item ->
            CategoryOption(
                id = item.id,
                label = item.label,
                isCustom = item.isCustom
            )
        }

        // Keep previous selection only when it still exists in refreshed list.
        selectedLabel = selectedLabel.takeIf { selected ->
            options.any { option -> option.matches(selected) }
        }.orEmpty()

        adapter.submitCategories(options, selectedLabel) {
            updateCategoryListHeight()
        }
        binding.deleteHint.isVisible = options.any { it.isCustom }
    }

    private fun updateCategoryListHeight() {
        val maxHeight = (resources.displayMetrics.heightPixels * CATEGORY_MAX_HEIGHT_RATIO).toInt()

        binding.categoryList.post {
            val categoryList = _binding?.categoryList ?: return@post
            val measuredContentHeight =
                categoryList.computeVerticalScrollRange() +
                        categoryList.paddingTop +
                        categoryList.paddingBottom
            val fallbackHeight = estimateCategoryListHeight(adapter.itemCount)
            val desiredHeight = measuredContentHeight.takeIf { it > 0 } ?: fallbackHeight
            val finalHeight = desiredHeight.coerceAtMost(maxHeight)

            categoryList.updateLayoutParams<ViewGroup.LayoutParams> {
                if (height != finalHeight) {
                    height = finalHeight
                }
            }
        }
    }

    private fun estimateCategoryListHeight(itemCount: Int): Int {
        val rows = maxOf(1, (itemCount + GRID_SPAN_COUNT - 1) / GRID_SPAN_COUNT)
        val rowHeight = dpToPx(CATEGORY_FALLBACK_ROW_HEIGHT_DP)
        val verticalGapCount = (rows - 1).coerceAtLeast(0)
        val verticalGaps = verticalGapCount * dpToPx(CATEGORY_ITEM_SPACING_VERTICAL_DP)
        return rows * rowHeight + verticalGaps
    }

    private fun showToast(@StringRes messageRes: Int) {
        if (!isAdded) return
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    private data class CategoryOption(
        val id: Long,
        val label: String,
        val isCustom: Boolean
    ) {
        fun matches(value: String): Boolean = label.equals(value, ignoreCase = true)
    }

    private sealed class CategoryListItem {
        data class Category(val option: CategoryOption) : CategoryListItem()
        object AddCategory : CategoryListItem()
    }

    private class CategoryChipAdapter(
        private val onSelect: (CategoryOption) -> Unit,
        private val onLongPress: (CategoryOption) -> Unit,
        private val onAddClick: () -> Unit
    ) : ListAdapter<CategoryListItem, RecyclerView.ViewHolder>(CategoryListItemDiff) {

        companion object {
            private const val VIEW_TYPE_CATEGORY = 1
            private const val VIEW_TYPE_ADD = 2
            private object SelectionPayload

            private val CategoryListItemDiff = object : DiffUtil.ItemCallback<CategoryListItem>() {
                override fun areItemsTheSame(
                    oldItem: CategoryListItem,
                    newItem: CategoryListItem
                ): Boolean {
                    return when {
                        oldItem is CategoryListItem.Category && newItem is CategoryListItem.Category ->
                            oldItem.option.id == newItem.option.id

                        oldItem is CategoryListItem.AddCategory && newItem is CategoryListItem.AddCategory ->
                            true

                        else -> false
                    }
                }

                override fun areContentsTheSame(
                    oldItem: CategoryListItem,
                    newItem: CategoryListItem
                ): Boolean {
                    return oldItem == newItem
                }
            }
        }

        private var selectedLabel: String = ""

        fun hasCategoryItems(): Boolean = currentList.any { it is CategoryListItem.Category }

        fun submitCategories(
            list: List<CategoryOption>,
            selected: String,
            onCommitted: (() -> Unit)? = null
        ) {
            val items = ArrayList<CategoryListItem>(list.size + 1).apply {
                addAll(list.map { CategoryListItem.Category(it) })
                add(CategoryListItem.AddCategory)
            }
            val previous = selectedLabel
            selectedLabel = selected
            submitList(items) {
                notifySelectionChanged(previous, selectedLabel)
                onCommitted?.invoke()
            }
        }

        fun updateSelection(label: String) {
            if (selectedLabel.equals(label, ignoreCase = true)) return
            val previous = selectedLabel
            selectedLabel = label
            notifySelectionChanged(previous, selectedLabel)
        }

        private fun notifySelectionChanged(previousLabel: String, currentLabel: String) {
            val previousIndex = indexOfCategoryByLabel(previousLabel)
            val currentIndex = indexOfCategoryByLabel(currentLabel)

            // Only refresh old/new selected rows, avoid full list redraw.
            if (previousIndex >= 0) notifyItemChanged(previousIndex, SelectionPayload)
            if (currentIndex >= 0 && currentIndex != previousIndex) {
                notifyItemChanged(currentIndex, SelectionPayload)
            }
        }

        private fun indexOfCategoryByLabel(label: String): Int {
            return currentList.indexOfFirst { item ->
                val categoryItem = item as? CategoryListItem.Category ?: return@indexOfFirst false
                categoryItem.option.matches(label)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is CategoryListItem.Category -> VIEW_TYPE_CATEGORY
                is CategoryListItem.AddCategory -> VIEW_TYPE_ADD
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_CATEGORY -> {
                    val binding = ItemCategoryChipBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    CategoryViewHolder(binding)
                }

                VIEW_TYPE_ADD -> {
                    val binding = ItemCategoryAddBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    AddViewHolder(binding)
                }

                else -> error("Unsupported viewType=$viewType")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is CategoryViewHolder -> {
                    val item = getItem(position) as CategoryListItem.Category
                    val option = item.option
                    holder.bind(
                        option = option,
                        selected = option.matches(selectedLabel),
                        onSelect = onSelect,
                        onLongPress = onLongPress
                    )
                }

                is AddViewHolder -> holder.bind(onAddClick)
            }
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.any { it === SelectionPayload } && holder is CategoryViewHolder) {
                val item = getItem(position) as? CategoryListItem.Category ?: return
                holder.bindSelection(item.option.matches(selectedLabel))
                return
            }
            super.onBindViewHolder(holder, position, payloads)
        }

        class CategoryViewHolder(
            private val binding: ItemCategoryChipBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(
                option: CategoryOption,
                selected: Boolean,
                onSelect: (CategoryOption) -> Unit,
                onLongPress: (CategoryOption) -> Unit
            ) {
                binding.chipText.text = option.label
                bindSelection(selected)
                binding.chipText.setOnClickListener { onSelect(option) }
                binding.chipText.setOnLongClickListener {
                    if (option.isCustom) {
                        binding.chipText.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        playDeleteAnimation(binding.chipText)
                        onLongPress(option)
                        true
                    } else {
                        false
                    }
                }
            }

            fun bindSelection(selected: Boolean) {
                binding.chipText.isSelected = selected
            }

            private fun playDeleteAnimation(target: View) {
                ObjectAnimator.ofFloat(target, View.ROTATION, 0f, -2.5f, 2.5f, -2f, 2f, 0f).apply {
                    duration = 260L
                    start()
                }
            }
        }

        class AddViewHolder(
            private val binding: ItemCategoryAddBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(onAddClick: () -> Unit) {
                binding.addText.setOnClickListener { onAddClick() }
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

            if (spanCount <= 1) {
                outRect.left = 0
                outRect.right = 0
                if (position > 0) outRect.top = verticalSpacing
                return
            }

            val column = position % spanCount
            val trailingSpacing = horizontalSpacing / 2
            val leadingSpacing = horizontalSpacing - trailingSpacing

            // Keep outer edges flush with container while preserving inter-item spacing.
            when (column) {
                0 -> {
                    outRect.left = 0
                    outRect.right = trailingSpacing
                }

                spanCount - 1 -> {
                    outRect.left = leadingSpacing
                    outRect.right = 0
                }

                else -> {
                    outRect.left = leadingSpacing
                    outRect.right = trailingSpacing
                }
            }

            // Add vertical gap starting from the second row.
            if (position >= spanCount) {
                outRect.top = verticalSpacing
            }
        }
    }
}
