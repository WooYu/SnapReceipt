package com.snapreceipt.io.ui.invoice.bottomsheet

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
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
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetInvoiceCategoryBinding
import com.snapreceipt.io.domain.manager.CategoryCacheManager
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.usecase.category.AddCategoryUseCase
import com.snapreceipt.io.domain.usecase.category.DeleteCategoryUseCase
import com.snapreceipt.io.domain.usecase.category.FetchCategoriesUseCase
import com.snapreceipt.io.ui.invoice.dialogs.CustomTypeDialog
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
        private const val CUSTOM_TYPE_DIALOG_TAG = "custom_type_dialog"

        // Keep three columns so each chip has enough touch area and text room.
        private const val GRID_SPAN_COUNT = 3
        private const val CATEGORY_ITEM_SPACING_HORIZONTAL_DP = 21f
        private const val CATEGORY_ITEM_SPACING_VERTICAL_DP = 20f

        fun newInstance(
            initialSelection: String?,
            onSelected: (String) -> Unit
        ): InvoiceCategoryBottomSheet {
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

    @Inject
    lateinit var categoryCache: CategoryCacheManager

    private var _binding: BottomSheetInvoiceCategoryBinding? = null
    private val binding: BottomSheetInvoiceCategoryBinding
        get() = checkNotNull(_binding) {
            "Binding is only valid between onCreateView and onDestroyView"
        }

    private var onSelected: ((String) -> Unit)? = null
    private val adapter by lazy {
        CategoryChipAdapter(
            onSelect = { option -> toggleSelection(option.label) },
            onLongPress = ::confirmDelete,
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
        selectedLabel = selectedLabel.trim()
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

        viewLifecycleOwner.lifecycleScope.launch {
            val cached = categoryCache.getCategories()
            applyCategories(cached, reconcileSelection = false)
            refreshCategoriesFromRemote()
        }
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
                // Use expanded + non-draggable mode to keep the action buttons stable.
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
            // Keep scroll fully inside RecyclerView to avoid parent nested-scroll arbitration jank.
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            // Avoid default change animations causing spacing/jump artifacts after insert refresh.
            itemAnimator = null
            if (itemDecorationCount == 0) {
                addItemDecoration(
                    GridSpacingItemDecoration(
                        spanCount = GRID_SPAN_COUNT,
                        horizontalSpacing = dpToPx(CATEGORY_ITEM_SPACING_HORIZONTAL_DP),
                        verticalSpacing = dpToPx(CATEGORY_ITEM_SPACING_VERTICAL_DP)
                    )
                )
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
            .show(parentFragmentManager, CUSTOM_TYPE_DIALOG_TAG)
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
                categoryCache.update(list)
                applyCategories(list, reconcileSelection = true)
            }.onFailure {
                // If remote fetch fails, keep showing cached data rather than an empty list.
                if (!adapter.hasCategoryItems()) {
                    val cached = categoryCache.getCategories()
                    if (cached.isNotEmpty()) {
                        applyCategories(cached, reconcileSelection = false)
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
        if (!option.isCustom) return
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

    private fun applyCategories(
        list: List<ReceiptCategory.Item>,
        reconcileSelection: Boolean
    ) {
        val options = list.map { item ->
            CategoryOption(
                id = item.id,
                label = item.label,
                isCustom = item.isCustom
            )
        }

        // Do not clear selection on cached/placeholder render.
        // Only reconcile on authoritative (remote) data to avoid losing preselection.
        if (reconcileSelection) {
            selectedLabel = selectedLabel.takeIf { selected ->
                options.any { option -> option.matches(selected) }
            }.orEmpty()
        }

        adapter.submitCategories(options, selectedLabel)
        binding.deleteHint.isVisible = options.any { it.isCustom }
    }

    private fun showToast(@StringRes messageRes: Int) {
        if (!isAdded) return
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }
}
