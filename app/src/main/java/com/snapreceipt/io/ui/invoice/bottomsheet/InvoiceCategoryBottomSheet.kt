package com.snapreceipt.io.ui.invoice.bottomsheet

import android.app.Dialog
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetInvoiceCategoryBinding
import com.snapreceipt.io.databinding.ItemCategoryCustomRowBinding
import com.snapreceipt.io.databinding.ItemCategoryRecommendRowBinding
import com.snapreceipt.io.domain.manager.CategoryCacheManager
import com.snapreceipt.io.domain.model.CategoryItem
import com.snapreceipt.io.domain.usecase.category.AddCategoryUseCase
import com.snapreceipt.io.domain.usecase.category.DeleteCategoryUseCase
import com.snapreceipt.io.domain.usecase.category.FetchCategoriesUseCase
import com.snapreceipt.io.ui.invoice.dialogs.CustomTypeDialog
import com.snapreceipt.io.ui.main.ListRefreshViewModel
import com.snapreceipt.io.ui.widget.bottomsheet.applyHorizontalMargins
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

    private val refreshViewModel: ListRefreshViewModel by activityViewModels()

    private var _binding: BottomSheetInvoiceCategoryBinding? = null
    private val binding: BottomSheetInvoiceCategoryBinding
        get() = checkNotNull(_binding) {
            "Binding is only valid between onCreateView and onDestroyView"
        }

    private var onSelected: ((String) -> Unit)? = null
    private var selectedLabel: String = ""
    private var refreshJob: Job? = null
    private var isOperationInProgress = false

    private var currentRecommendItems: List<CategoryItem> = emptyList()
    private var currentCustomItems: List<CategoryItem> = emptyList()

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
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        onSelected = null
        super.onDestroy()
    }

    private fun setupBottomSheet(dialog: BottomSheetDialog) {
        dialog.applyHorizontalMargins(resources.getDimensionPixelSize(R.dimen.page_start_margin)) { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
        }
    }

    private fun setupActions() {
        binding.addCategoryBtn.setOnClickListener {
            if (!isOperationInProgress) openAddCategoryDialog()
        }

        binding.cancelBtn.text = getString(R.string.reset)
        binding.cancelBtn.setOnClickListener {
            selectedLabel = ""
            updateSelectionUI()
        }
        binding.confirmBtn.setOnClickListener {
            onSelected?.invoke(selectedLabel)
            dismiss()
        }
    }

    private fun openAddCategoryDialog() {
        CustomTypeDialog { customType -> addCustomType(customType) }
            .show(parentFragmentManager, CUSTOM_TYPE_DIALOG_TAG)
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
                if (currentRecommendItems.isEmpty() && currentCustomItems.isEmpty()) {
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
        if (sanitized.isBlank() || isOperationInProgress) return
        isOperationInProgress = true

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(dispatchers.io) { addCategoryUseCase(sanitized) }
            result.onSuccess {
                selectedLabel = sanitized
                refreshCategoriesFromRemote()
            }.onFailure {
                showToast(R.string.add_category_failed)
            }
            isOperationInProgress = false
        }
    }

    private fun confirmDelete(item: CategoryItem) {
        if (isOperationInProgress) return
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.delete_category_confirm, item.label))
            .setPositiveButton(R.string.confirm) { _, _ -> deleteCategory(item) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteCategory(item: CategoryItem) {
        if (isOperationInProgress) return
        isOperationInProgress = true

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(dispatchers.io) { deleteCategoryUseCase(listOf(item.id)) }
            result.onSuccess {
                if (selectedLabel.equals(item.label, ignoreCase = true)) {
                    selectedLabel = ""
                }
                refreshCategoriesFromRemote()
                refreshViewModel.markHomeDirty()
                refreshViewModel.markReceiptsDirty()
            }.onFailure {
                showToast(R.string.delete_category_failed)
            }
            isOperationInProgress = false
        }
    }

    private fun applyCategories(
        list: List<CategoryItem>,
        reconcileSelection: Boolean
    ) {
        currentRecommendItems = list.filter { !it.isCustom }
        currentCustomItems = list.filter { it.isCustom }

        if (reconcileSelection) {
            selectedLabel = selectedLabel.takeIf { selected ->
                list.any { it.label.equals(selected, ignoreCase = true) }
            }.orEmpty()
        }

        buildRecommendRows()
        buildCustomRows()
    }

    private fun buildRecommendRows() {
        val container = binding.recommendContainer
        container.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())
        currentRecommendItems.forEach { item ->
            val rowBinding = ItemCategoryRecommendRowBinding.inflate(inflater, container, false)
            rowBinding.rowText.text = item.label

            val isSelected = item.label.equals(selectedLabel, ignoreCase = true)
            applyRecommendRowSelection(rowBinding, isSelected)

            rowBinding.rowContent.setOnClickListener {
                toggleSelection(item.label)
            }
            container.addView(rowBinding.root)
        }
    }

    private fun buildCustomRows() {
        val container = binding.customContainer
        container.removeAllViews()

        binding.customizeHeader.isVisible = currentCustomItems.isNotEmpty()

        val inflater = LayoutInflater.from(requireContext())
        currentCustomItems.forEach { item ->
            val rowBinding = ItemCategoryCustomRowBinding.inflate(inflater, container, false)
            rowBinding.customRowText.text = item.label

            val isSelected = item.label.equals(selectedLabel, ignoreCase = true)
            applyCustomRowSelection(rowBinding, isSelected)

            rowBinding.customRowDelete.setOnClickListener { confirmDelete(item) }
            rowBinding.customRowRoot.setOnClickListener {
                toggleSelection(item.label)
            }
            container.addView(rowBinding.root)
        }
    }

    private fun toggleSelection(label: String) {
        selectedLabel = if (label.equals(selectedLabel, ignoreCase = true)) {
            ""
        } else {
            label
        }
        updateSelectionUI()
    }

    private fun updateSelectionUI() {
        val recommendContainer = binding.recommendContainer
        for (i in 0 until recommendContainer.childCount) {
            val child = recommendContainer.getChildAt(i)
            val rowBinding = ItemCategoryRecommendRowBinding.bind(child)
            val label = rowBinding.rowText.text.toString()
            val isSelected = label.equals(selectedLabel, ignoreCase = true)
            applyRecommendRowSelection(rowBinding, isSelected)
        }

        val customContainer = binding.customContainer
        for (i in 0 until customContainer.childCount) {
            val child = customContainer.getChildAt(i)
            val rowBinding = ItemCategoryCustomRowBinding.bind(child)
            val label = rowBinding.customRowText.text.toString()
            val isSelected = label.equals(selectedLabel, ignoreCase = true)
            applyCustomRowSelection(rowBinding, isSelected)
        }
    }

    private fun applyRecommendRowSelection(
        rowBinding: ItemCategoryRecommendRowBinding,
        isSelected: Boolean
    ) {
        if (isSelected) {
            rowBinding.rowContent.setBackgroundResource(R.drawable.bg_category_row_selected)
            rowBinding.rowText.setTextColor(
                resources.getColor(R.color.colorPrimary, requireContext().theme)
            )
            rowBinding.rowCheck.isVisible = true
        } else {
            rowBinding.rowContent.background = null
            rowBinding.rowText.setTextColor(
                resources.getColor(R.color.text_primary, requireContext().theme)
            )
            rowBinding.rowCheck.isVisible = false
        }
    }

    private fun applyCustomRowSelection(
        rowBinding: ItemCategoryCustomRowBinding,
        isSelected: Boolean
    ) {
        if (isSelected) {
            rowBinding.customRowRoot.setBackgroundResource(R.drawable.bg_option_row_selected)
            rowBinding.customRowText.setTextColor(
                resources.getColor(R.color.colorPrimary, requireContext().theme)
            )
        } else {
            rowBinding.customRowRoot.setBackgroundResource(R.drawable.bg_custom_category_row)
            rowBinding.customRowText.setTextColor(
                resources.getColor(R.color.text_primary, requireContext().theme)
            )
        }
    }

    private fun showToast(@StringRes messageRes: Int) {
        if (!isAdded) return
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}
