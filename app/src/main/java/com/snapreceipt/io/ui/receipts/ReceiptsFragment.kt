package com.snapreceipt.io.ui.receipts

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.skybound.space.base.presentation.BaseFragment
import com.skybound.space.base.presentation.observeState
import com.skybound.space.core.config.AppConfig
import com.skybound.space.core.util.DateFormatUtil
import com.skybound.space.core.util.LogHelper
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.FragmentReceiptsBinding
import com.snapreceipt.io.data.manager.CategoryCacheManager
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity
import com.snapreceipt.io.ui.invoice.InvoiceDetailsArgsCodec
import com.snapreceipt.io.ui.invoice.bottomsheet.InvoiceCategoryBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.TitleTypeBottomSheet
import com.snapreceipt.io.ui.main.ListRefreshViewModel
import com.snapreceipt.io.ui.main.ReceiptsRefreshEvent
import com.snapreceipt.io.ui.receipts.dialogs.ExportSuccessDialog
import com.snapreceipt.io.ui.widget.datepicker.DateRangeBottomSheet
import com.snapreceipt.io.ui.widget.statefullist.StatefulListLayout
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceiptsFragment : BaseFragment<ReceiptsViewModel>(R.layout.fragment_receipts) {
    companion object {
        private const val LOG_TAG = "ReceiptsFilter"
        private const val KEY_FILTER_START = "filter_start_millis"
        private const val KEY_FILTER_END = "filter_end_millis"
        private const val KEY_FILTER_TYPE = "filter_type_label"
        private const val KEY_FILTER_CATEGORY = "filter_category_label"
    }

    override val viewModel: ReceiptsViewModel by viewModels()
    private val refreshViewModel: ListRefreshViewModel by activityViewModels()

    @Inject
    lateinit var categoryCache: CategoryCacheManager

    private var _binding: FragmentReceiptsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReceiptsSelectableAdapter
    private var filterStartMillis: Long? = null
    private var filterEndMillis: Long? = null
    private var filterTypeLabel: String? = null
    private var filterCategoryLabel: String? = null
    private var currentState: ReceiptsUiState = ReceiptsUiState()
    private var shouldScrollToTopOnNextRender = false
    private var pendingReceiptsRefreshAfterLoad = false
    private var pendingReceiptsRefreshReason: String? = null

    // 用于启动 InvoiceDetailsActivity 并处理返回结果
    private val invoiceDetailsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            lifecycleScope.launch {
                handleInvoiceResult(
                    operationType = data.getStringExtra(InvoiceDetailsArgsCodec.EXTRA_OPERATION_TYPE),
                    receipt = IntentCompat.getParcelableExtra(
                        data,
                        InvoiceDetailsArgsCodec.EXTRA_RECEIPT,
                        ReceiptEntity::class.java
                    ),
                    receiptId = data.getLongExtra(InvoiceDetailsArgsCodec.EXTRA_RECEIPT_ID, -1L)
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentReceiptsBinding.bind(view)
        setupAdapter()
        setupListeners()
        observeRefreshEvents()
        restoreFilterState(savedInstanceState)
        observeState(viewModel.uiState) { renderState(it) }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_FILTER_START, filterStartMillis ?: -1L)
        outState.putLong(KEY_FILTER_END, filterEndMillis ?: -1L)
        outState.putString(KEY_FILTER_TYPE, filterTypeLabel)
        outState.putString(KEY_FILTER_CATEGORY, filterCategoryLabel)
    }

    override fun onResume() {
        super.onResume()
        if (refreshViewModel.consumeReceiptsDirty()) {
            if (!syncCategoryFilterDisplayIfInvalid("on_resume_consume_receipts_dirty")) {
                requestReceiptsRefreshOrDefer("on_resume_consume_receipts_dirty")
            }
        }
    }

    private fun restoreFilterState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return

        val savedStart = savedInstanceState.getLong(KEY_FILTER_START, -1L)
        val savedEnd = savedInstanceState.getLong(KEY_FILTER_END, -1L)
        filterStartMillis = savedStart.takeIf { it > 0L }
        filterEndMillis = savedEnd.takeIf { it > 0L }
        filterTypeLabel = savedInstanceState.getString(KEY_FILTER_TYPE)
        filterCategoryLabel = savedInstanceState.getString(KEY_FILTER_CATEGORY)

        if (filterStartMillis != null && filterEndMillis != null) {
            binding.filterDateBtn.text = formatDateRange(filterStartMillis!!, filterEndMillis!!)
        }
        if (!filterCategoryLabel.isNullOrBlank()) {
            binding.filterCategoryBtn.text = filterCategoryLabel
        }
        if (!filterTypeLabel.isNullOrBlank()) {
            binding.filterTypeBtn.text = filterTypeLabel
        }

        if (hasActiveFilters()) {
            viewModel.restoreQuery(filterStartMillis, filterEndMillis, filterCategoryLabel, filterTypeLabel)
        }
    }

    private fun hasActiveFilters(): Boolean {
        return filterStartMillis != null ||
                filterEndMillis != null ||
                !filterTypeLabel.isNullOrBlank() ||
                !filterCategoryLabel.isNullOrBlank()
    }

    /**
     * Renders UI state.
     * Data is submitted before footer state to prevent auto-scroll when footer appears.
     */
    private fun renderState(state: ReceiptsUiState) {
        tryConsumePendingReceiptsRefresh(state)
        currentState = state

        // Determine which data to show
        val receiptsToShow = if (state.hasLoaded && state.empty) emptyList() else state.receipts

        // Submit data first (async), then update footer state in callback
        adapter.setReceipts(receiptsToShow) {
            val binding = _binding ?: return@setReceipts
            if (shouldScrollToTopOnNextRender && receiptsToShow.isNotEmpty()) {
                binding.statefulList.recyclerView.scrollToPosition(0)
                shouldScrollToTopOnNextRender = false
            }
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
            .fold(java.math.BigDecimal.ZERO) { acc, r -> acc + (r.totalAmount ?: java.math.BigDecimal.ZERO) }
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

    private fun requestReceiptsRefreshOrDefer(trigger: String) {
        val state = viewModel.uiState.value
        LogHelper.d(
            LOG_TAG,
            "request trigger=$trigger loading=${state.loading} refreshing=${state.refreshing} hasLoaded=${state.hasLoaded} hasActiveFilters=${hasActiveFilters()}"
        )
        if (!state.loading && !state.refreshing) {
            pendingReceiptsRefreshAfterLoad = false
            pendingReceiptsRefreshReason = null
            if (state.hasLoaded) {
                LogHelper.d(LOG_TAG, "execute trigger=$trigger action=refresh")
                viewModel.refresh()
            } else {
                LogHelper.d(LOG_TAG, "execute trigger=$trigger action=load")
                viewModel.loadReceipts()
            }
        } else {
            pendingReceiptsRefreshAfterLoad = true
            pendingReceiptsRefreshReason = trigger
            LogHelper.d(LOG_TAG, "defer trigger=$trigger")
        }
    }

    private fun tryConsumePendingReceiptsRefresh(state: ReceiptsUiState) {
        if (!pendingReceiptsRefreshAfterLoad) return
        if (state.loading || state.refreshing) return
        val deferredReason = pendingReceiptsRefreshReason ?: "unknown"
        pendingReceiptsRefreshAfterLoad = false
        pendingReceiptsRefreshReason = null
        if (state.hasLoaded) {
            LogHelper.d(LOG_TAG, "consume deferred=$deferredReason action=refresh")
            viewModel.refresh()
        } else {
            LogHelper.d(LOG_TAG, "consume deferred=$deferredReason action=load")
            viewModel.loadReceipts()
        }
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
        binding.statefulList.setOnRefreshListener {
            LogHelper.d(LOG_TAG, "ui pull_to_refresh")
            viewModel.refresh()
        }
        binding.statefulList.setOnLoadMoreListener {
            LogHelper.d(LOG_TAG, "ui load_more")
            viewModel.loadMore()
        }
        binding.statefulList.setOnRetryListener {
            LogHelper.d(LOG_TAG, "ui retry_load")
            viewModel.refresh()
        }
    }

    private fun setupListeners() {
        binding.filterDateBtn.setOnClickListener {
            DateRangeBottomSheet.newInstance(filterStartMillis, filterEndMillis) { start, end ->
                if (start == null || end == null) {
                    filterStartMillis = null
                    filterEndMillis = null
                    binding.filterDateBtn.text = getString(R.string.select_date)
                    LogHelper.d(LOG_TAG, "Date filter reset")
                    viewModel.clearDateRangeFilter()
                    return@newInstance
                }
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
            TitleTypeBottomSheet.newInstance(initial) { selected ->
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

    private fun observeRefreshEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                refreshViewModel.refreshReceiptsEvent.collect { event ->
                    when (event) {
                        is ReceiptsRefreshEvent.ItemAdded -> {
                            if (hasActiveFilters()) {
                                requestReceiptsRefreshOrDefer(
                                    "receipts_refresh_event_item_added_with_filters"
                                )
                            } else {
                                LogHelper.d(LOG_TAG, "event item_added -> local insert")
                                viewModel.addReceiptLocally(event.receipt)
                                shouldScrollToTopOnNextRender = true
                            }
                        }
                        is ReceiptsRefreshEvent.ItemUpdated -> {
                            if (hasActiveFilters()) {
                                requestReceiptsRefreshOrDefer(
                                    "receipts_refresh_event_item_updated_with_filters"
                                )
                            } else {
                                LogHelper.d(LOG_TAG, "event item_updated -> local update")
                                viewModel.updateReceiptLocally(event.receipt)
                            }
                        }
                        is ReceiptsRefreshEvent.ItemDeleted -> {
                            if (hasActiveFilters()) {
                                requestReceiptsRefreshOrDefer(
                                    "receipts_refresh_event_item_deleted_with_filters"
                                )
                            } else {
                                LogHelper.d(LOG_TAG, "event item_deleted -> local delete")
                                viewModel.deleteReceiptLocally(event.receiptId)
                            }
                        }
                        is ReceiptsRefreshEvent.FullRefresh -> {
                            refreshViewModel.consumeReceiptsDirty()
                            if (!syncCategoryFilterDisplayIfInvalid(
                                    "receipts_refresh_event_full_refresh"
                                )
                            ) {
                                requestReceiptsRefreshOrDefer(
                                    "receipts_refresh_event_full_refresh"
                                )
                            }
                        }
                    }
                }
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
        viewModel.filterByInvoiceCategory(filterCategoryLabel.orEmpty())
    }

    private fun syncCategoryFilterDisplayIfInvalid(trigger: String): Boolean {
        val currentLabel = filterCategoryLabel?.trim().orEmpty()
        if (currentLabel.isBlank()) return false
        if (categoryCache.getCategories().isEmpty()) {
            LogHelper.d(
                LOG_TAG,
                "Defer category filter validation to ViewModel trigger=$trigger reason=empty_cache label='$currentLabel'"
            )
            viewModel.filterByInvoiceCategory(currentLabel)
            return true
        }
        if (categoryCache.idForLabel(currentLabel) > 0L) return false

        filterCategoryLabel = null
        binding.filterCategoryBtn.text = getString(R.string.filter_category)
        LogHelper.d(
            LOG_TAG,
            "Category filter '$currentLabel' no longer exists, reset filter state trigger=$trigger"
        )
        viewModel.filterByInvoiceCategory("")
        return true
    }

    private fun applyTypeFilterSelection(rawSelected: String) {
        val normalized = rawSelected.trim()
        filterTypeLabel = normalized.ifBlank { null }
        binding.filterTypeBtn.text = filterTypeLabel ?: getString(R.string.filter_type)
        LogHelper.d(
            LOG_TAG,
            "Type filter selected='$normalized', applied='${filterTypeLabel ?: getString(R.string.filter_type)}'"
        )
        viewModel.filterByReceiptType(filterTypeLabel)
    }

    private fun openReceiptDetails(receipt: ReceiptEntity) {
        val intent = com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.createIntent(
            requireContext(),
            receipt,
            com.snapreceipt.io.ui.invoice.InvoiceDetailsArgsCodec.SOURCE_INVOICES_LIST
        )
        invoiceDetailsLauncher.launch(intent)
    }

    private fun formatDateRange(start: Long, end: Long): String {
        return getString(
            R.string.date_range_format,
            DateFormatUtil.formatDisplayDate(start),
            DateFormatUtil.formatDisplayDate(end)
        )
    }

    override fun onCustomEvent(event: com.skybound.space.base.presentation.UiEvent.Custom) {
        when (event.type) {
            ReceiptsEventKeys.SHOW_EXPORT_SUCCESS -> {
                resetAllFiltersAfterExport()
                val exportUrl = event.payload?.getString(ReceiptsEventKeys.EXPORT_URL).orEmpty()
                ExportSuccessDialog {
                    openExportUrl(exportUrl)
                }.show(parentFragmentManager, "export_success")
            }
            ReceiptsEventKeys.CATEGORY_FILTER_INVALID -> {
                if (_binding == null) return
                val invalidLabel = event.payload?.getString(ReceiptsEventKeys.CATEGORY_LABEL).orEmpty()
                if (filterCategoryLabel.equals(invalidLabel, ignoreCase = true)) {
                    filterCategoryLabel = null
                    binding.filterCategoryBtn.text = getString(R.string.filter_category)
                    LogHelper.d(
                        LOG_TAG,
                        "Category filter '$invalidLabel' cleared by ViewModel invalid event"
                    )
                }
            }
        }
    }

    private fun resetAllFiltersAfterExport() {
        filterStartMillis = null
        filterEndMillis = null
        filterTypeLabel = null
        filterCategoryLabel = null

        if (_binding != null) {
            binding.filterDateBtn.text = getString(R.string.select_date)
            binding.filterCategoryBtn.text = getString(R.string.filter_category)
            binding.filterTypeBtn.text = getString(R.string.filter_type)
        }

        pendingReceiptsRefreshAfterLoad = false
        pendingReceiptsRefreshReason = null
        LogHelper.d(LOG_TAG, "Export success -> reset all filters and reload receipts")
        viewModel.loadReceipts()
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
        } else if (state.loading && state.hasLoaded) {
            showLoading(true, getString(R.string.loading_please_wait_dynamic))
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

    private suspend fun handleInvoiceResult(
        operationType: String?,
        receipt: ReceiptEntity?,
        receiptId: Long
    ) {
        when (operationType) {
            InvoiceDetailsArgsCodec.OPERATION_TYPE_ADD -> {
                if (hasActiveFilters()) {
                    requestReceiptsRefreshOrDefer("invoice_details_result_add_with_filters")
                    refreshViewModel.notifyHomeFullRefresh()
                } else if (receipt != null && (receipt.receiptId?.let { it > 0L } == true)) {
                    LogHelper.d(LOG_TAG, "invoice result add -> local insert + markHomeDirty")
                    viewModel.addReceiptLocally(receipt)
                    shouldScrollToTopOnNextRender = true
                    refreshViewModel.notifyHomeItemAdded(receipt)
                } else {
                    LogHelper.d(LOG_TAG, "invoice result add missing payload/id -> full refresh")
                    requestReceiptsRefreshOrDefer("invoice_details_result_add_missing_payload_or_id")
                    refreshViewModel.notifyHomeFullRefresh()
                }
            }
            InvoiceDetailsArgsCodec.OPERATION_TYPE_UPDATE -> {
                if (hasActiveFilters()) {
                    requestReceiptsRefreshOrDefer("invoice_details_result_update_with_filters")
                    refreshViewModel.notifyHomeFullRefresh()
                } else if (receipt != null && (receipt.receiptId?.let { it > 0L } == true)) {
                    LogHelper.d(LOG_TAG, "invoice result update -> local update + markHomeDirty")
                    viewModel.updateReceiptLocally(receipt)
                    refreshViewModel.notifyHomeItemUpdated(receipt)
                } else {
                    LogHelper.d(LOG_TAG, "invoice result update missing payload/id -> full refresh")
                    requestReceiptsRefreshOrDefer(
                        "invoice_details_result_update_missing_payload_or_id"
                    )
                    refreshViewModel.notifyHomeFullRefresh()
                }
            }
            InvoiceDetailsArgsCodec.OPERATION_TYPE_DELETE -> {
                if (hasActiveFilters()) {
                    requestReceiptsRefreshOrDefer("invoice_details_result_delete_with_filters")
                    refreshViewModel.notifyHomeFullRefresh()
                } else if (receiptId > 0L) {
                    LogHelper.d(LOG_TAG, "invoice result delete -> local delete + markHomeDirty")
                    viewModel.deleteReceiptLocally(receiptId)
                    refreshViewModel.notifyHomeItemDeleted(receiptId)
                } else {
                    requestReceiptsRefreshOrDefer("invoice_details_result_delete_missing_payload")
                    refreshViewModel.notifyHomeFullRefresh()
                }
            }
            else -> {
                requestReceiptsRefreshOrDefer("invoice_details_result_unknown_operation")
                refreshViewModel.notifyHomeFullRefresh()
            }
        }
    }
}
