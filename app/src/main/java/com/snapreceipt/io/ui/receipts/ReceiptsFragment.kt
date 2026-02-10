package com.snapreceipt.io.ui.receipts

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.skybound.space.base.presentation.BaseFragment
import com.skybound.space.base.presentation.observeState
import com.skybound.space.core.config.AppConfig
import com.skybound.space.core.util.DateFormatUtil
import com.skybound.space.core.util.LogHelper
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.FragmentReceiptsBinding
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.ui.invoice.bottomsheet.InvoiceCategoryBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.TitleTypeBottomSheet
import com.snapreceipt.io.ui.receipts.dialogs.ExportSuccessDialog
import com.snapreceipt.io.ui.widget.datepicker.DateRangeBottomSheet
import com.snapreceipt.io.ui.widget.statefullist.StatefulListLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReceiptsFragment : BaseFragment<ReceiptsViewModel>(R.layout.fragment_receipts) {
    companion object {
        private const val LOG_TAG = "ReceiptsFilter"
    }

    override val viewModel: ReceiptsViewModel by viewModels()

    private var _binding: FragmentReceiptsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReceiptsSelectableAdapter
    private var filterStartMillis: Long? = null
    private var filterEndMillis: Long? = null
    private var filterTypeLabel: String? = null
    private var filterCategoryLabel: String? = null
    private var currentState: ReceiptsUiState = ReceiptsUiState()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentReceiptsBinding.bind(view)
        setupAdapter()
        setupListeners()
        observeState(viewModel.uiState) { renderState(it) }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (!ReceiptsRefreshSignal.consumeRefresh()) return
        if (currentState.loading || currentState.refreshing) return
        if (currentState.hasLoaded) {
            viewModel.refresh()
        } else {
            viewModel.loadReceipts()
        }
    }

    /**
     * Renders UI state.
     * Data is submitted before footer state to prevent auto-scroll when footer appears.
     */
    private fun renderState(state: ReceiptsUiState) {
        currentState = state

        // Determine which data to show
        val receiptsToShow = if (state.hasLoaded && state.empty) emptyList() else state.receipts

        // Submit data first (async), then update footer state in callback
        adapter.setReceipts(receiptsToShow) {
            binding.statefulList.submit(buildListState(state))
        }

        // Update toolbar title
        val selectedCount = state.selectedIds.size
        binding.toolbarTitle.text = if (selectedCount > 0) {
            getString(R.string.selected_count, selectedCount)
        } else {
            getString(R.string.receipts_title)
        }

        // Update action bar visibility
        binding.actionBar.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE

        // Update selection states
        adapter.updateSelection(state.selectedIds)

        // Calculate selected total amount
        val selectedTotal = state.receipts
            .filter { receipt ->
                val id = receipt.receiptId ?: return@filter false
                state.selectedIds.contains(id)
            }
            .sumOf { it.totalAmount ?: 0.0 }
        binding.totalAmount.text = getString(R.string.amount_currency_format, selectedTotal)

        // Update select-all checkbox state
        val allSelected =
            state.receipts.isNotEmpty() && state.selectedIds.size == state.receipts.size
        binding.selectAllIcon.isSelected = allSelected

        // Control modal loading dialog
        updateLoadingDialog(state)

        // Control export button state
        binding.exportActionBtn.isEnabled = !state.exporting
        binding.exportActionBtn.alpha = if (state.exporting) 0.6f else 1f
        binding.selectAllBtn.isEnabled = !state.exporting
    }

    private fun setupAdapter() {
        adapter = ReceiptsSelectableAdapter(
            selectedIds = emptySet(),
            onToggle = { id ->
                viewModel.toggleSelection(id)
            },
            onEditClick = { receipt ->
                openReceiptDetails(receipt)
            }
        )
        binding.statefulList.setAdapter(adapter)
        binding.statefulList.setOnRefreshListener { viewModel.refresh() }
        binding.statefulList.setOnLoadMoreListener { viewModel.loadMore() }
        binding.statefulList.setOnRetryListener { viewModel.refresh() }
    }

    private fun setupListeners() {
        binding.filterDateBtn.setOnClickListener {
            DateRangeBottomSheet(filterStartMillis, filterEndMillis) { start, end ->
                filterStartMillis = start
                filterEndMillis = end
                binding.filterDateBtn.text = formatDateRange(start, end)
                LogHelper.d(
                    LOG_TAG,
                    "Date filter selected start=${DateFormatUtil.formatApiDate(start)} end=${
                        DateFormatUtil.formatApiDate(
                            end
                        )
                    }"
                )
                viewModel.filterByDateRange(start, end)
            }.show(parentFragmentManager, "date_range_picker")
        }
        binding.filterCategoryBtn.setOnClickListener {
            val initial = filterCategoryLabel.orEmpty()
            InvoiceCategoryBottomSheet.newInstance(initial) { selected ->
                applyCategoryFilterSelection(selected)
            }.show(parentFragmentManager, "category_filter_picker")
        }
        binding.filterTypeBtn.setOnClickListener {
            val initial = filterTypeLabel.orEmpty()
            TitleTypeBottomSheet(initial) { selected ->
                applyTypeFilterSelection(selected)
            }.show(parentFragmentManager, "type_filter_picker")
        }
        binding.exportActionBtn.setOnClickListener {
            viewModel.exportSelected()
        }
        binding.selectAllBtn.setOnClickListener {
            val isAllSelected = currentState.receipts.isNotEmpty() &&
                    currentState.selectedIds.size == currentState.receipts.size
            if (isAllSelected) {
                viewModel.clearSelection()
            } else {
                viewModel.selectAll()
            }
        }
    }

    private fun applyCategoryFilterSelection(rawSelected: String) {
        val normalized = rawSelected.trim()
        filterCategoryLabel = normalized.ifBlank { null }
        binding.filterCategoryBtn.text = filterCategoryLabel ?: getString(R.string.filter_category)
        LogHelper.d(
            LOG_TAG,
            "Title filter selected='$normalized', applied='${filterCategoryLabel ?: getString(R.string.filter_category)}'"
        )
        viewModel.filterByInvoiceCategory(filterTypeLabel.orEmpty())
    }

    private fun applyTypeFilterSelection(rawSelected: String) {
        val normalized = rawSelected.trim()
        filterTypeLabel = normalized.ifBlank { null }
        binding.filterTypeBtn.text = filterTypeLabel ?: getString(R.string.filter_type)
        LogHelper.d(
            LOG_TAG,
            "Type filter selected='$normalized', applied='${filterTypeLabel ?: getString(R.string.filter_type)}'"
        )
        viewModel.filterByReceiptType(filterCategoryLabel)
    }

    private fun openReceiptDetails(receipt: ReceiptEntity) {
        startActivity(
            com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.createIntent(
                requireContext(),
                receipt
            )
        )
    }

    private fun formatDateRange(start: Long, end: Long): String {
        return getString(
            R.string.date_range_format,
            DateFormatUtil.formatDisplayDate(start),
            DateFormatUtil.formatDisplayDate(end)
        )
    }

    override fun onCustomEvent(event: com.skybound.space.base.presentation.UiEvent.Custom) {
        if (event.type == ReceiptsEventKeys.SHOW_EXPORT_SUCCESS) {
            val exportUrl = event.payload?.getString(ReceiptsEventKeys.EXPORT_URL).orEmpty()
            ExportSuccessDialog {
                openExportUrl(exportUrl)
            }.show(parentFragmentManager, "export_success")
        }
    }

    private fun openExportUrl(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.export_file_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val url = if (trimmed.startsWith("http", ignoreCase = true)) {
            trimmed
        } else {
            val base = AppConfig.baseUrl.trimEnd('/')
            val path = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
            "$base$path"
        }
        Toast.makeText(
            requireContext(),
            getString(R.string.opening_export_file),
            Toast.LENGTH_SHORT
        ).show()
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(requireContext(), getString(R.string.no_app_to_open), Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * 控制模态 loading 弹窗。
     * 仅导出操作显示模态 loading，列表加载/刷新由 StatefulListLayout 内置 loading 展示。
     */
    private fun updateLoadingDialog(state: ReceiptsUiState) {
        if (state.exporting) {
            showLoading(true, getString(R.string.exporting_receipts))
        } else {
            showLoading(false)
        }
    }

    private fun buildListState(state: ReceiptsUiState): StatefulListLayout.State {
        val contentState = when {
            state.loading && !state.hasLoaded -> StatefulListLayout.ContentState.LOADING
            !state.error.isNullOrBlank() && state.receipts.isEmpty() ->
                StatefulListLayout.ContentState.ERROR

            state.hasLoaded && state.empty -> StatefulListLayout.ContentState.EMPTY
            else -> StatefulListLayout.ContentState.CONTENT
        }
        val showNoMore =
            state.hasLoaded && !state.hasMore && state.receipts.isNotEmpty() && !state.loadingMore
        return StatefulListLayout.State(
            contentState = contentState,
            refreshing = state.refreshing,
            loadingMore = state.loadingMore,
            noMore = showNoMore,
            errorText = state.error
        )
    }
}
