package com.snapreceipt.io.ui.receipts

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.FragmentReceiptsBinding
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.ui.common.shouldShowEmpty
import com.snapreceipt.io.ui.common.shouldShowNoMore
import com.snapreceipt.io.ui.invoice.bottomsheet.InvoiceCategoryBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.TitleTypeBottomSheet
import com.snapreceipt.io.ui.receipts.bottomsheet.DateRangeBottomSheet
import com.snapreceipt.io.ui.receipts.dialogs.ExportSuccessDialog
import com.skybound.space.core.config.AppConfig
import com.skybound.space.core.util.DateFormatUtil
import com.skybound.space.base.presentation.BaseFragment
import com.skybound.space.base.presentation.observeState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReceiptsFragment : BaseFragment<ReceiptsViewModel>(R.layout.fragment_receipts) {
    override val viewModel: ReceiptsViewModel by viewModels()

    private var _binding: FragmentReceiptsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReceiptsSelectableAdapter
    private var filterStartMillis: Long? = null
    private var filterEndMillis: Long? = null
    private var filterTypeLabel: String? = null
    private var filterTitleLabel: String? = null
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
        viewModel.loadReceipts()
    }

    private fun renderState(state: ReceiptsUiState) {
        currentState = state
        val showEmpty = shouldShowEmpty(state.hasLoaded, state.empty)
        if (showEmpty) {
            binding.emptyState.visibility = View.VISIBLE
            binding.receiptList.visibility = View.GONE
            adapter.setReceipts(emptyList())
        } else {
            binding.emptyState.visibility = View.GONE
            binding.receiptList.visibility = View.VISIBLE
            adapter.setReceipts(state.receipts)
        }
        binding.swipeRefresh.isRefreshing = state.refreshing
        binding.loadMoreIndicator.visibility = if (state.loadingMore) View.VISIBLE else View.GONE
        binding.noMoreHint.visibility = if (
            shouldShowNoMore(state.hasLoaded, state.hasMore, state.receipts.size, state.loadingMore)
        ) View.VISIBLE else View.GONE
        val selectedCount = state.selectedIds.size
        binding.toolbarTitle.text = if (selectedCount > 0) {
            getString(R.string.selected_count, selectedCount)
        } else {
            getString(R.string.receipts_title)
        }
        binding.actionBar.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
        adapter.updateSelection(state.selectedIds)

        val selectedTotal = state.receipts
            .filter { receipt ->
                val id = receipt.receiptId ?: return@filter false
                state.selectedIds.contains(id)
            }
            .sumOf { it.totalAmount ?: 0.0 }
        binding.totalAmount.text = getString(R.string.amount_currency_format, selectedTotal)

        val allSelected = state.receipts.isNotEmpty() && state.selectedIds.size == state.receipts.size
        binding.selectAllIcon.isSelected = allSelected

        binding.exportLoadingOverlay.visibility = if (state.exporting) View.VISIBLE else View.GONE
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
        val layoutManager = binding.receiptList.layoutManager as LinearLayoutManager
        binding.receiptList.adapter = adapter
        binding.receiptList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val total = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (total > 0 && lastVisible >= total - 3) {
                    viewModel.loadMore()
                }
            }
        })
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.filterDateBtn.setOnClickListener {
            DateRangeBottomSheet(filterStartMillis, filterEndMillis) { start, end ->
                filterStartMillis = start
                filterEndMillis = end
                binding.filterDateBtn.text = formatDateRange(start, end)
                viewModel.filterByDateRange(start, end)
            }.show(parentFragmentManager, "date_range_picker")
        }
        binding.filterTypeBtn.setOnClickListener {
            val initial = filterTypeLabel ?: binding.filterTypeBtn.text.toString()
            InvoiceCategoryBottomSheet.newInstance(initial) { selected ->
                filterTypeLabel = selected
                binding.filterTypeBtn.text = selected
                viewModel.filterByType(selected)
            }.show(parentFragmentManager, "type_filter_picker")
        }
        binding.filterTitleBtn.setOnClickListener {
            val initial = filterTitleLabel ?: binding.filterTitleBtn.text.toString()
            TitleTypeBottomSheet(initial) { selected ->
                filterTitleLabel = selected
                binding.filterTitleBtn.text = selected
                viewModel.filterByTitleType(selected)
            }.show(parentFragmentManager, "title_filter_picker")
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

    private fun openReceiptDetails(receipt: ReceiptEntity) {
        startActivity(
            com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.createIntent(requireContext(), receipt)
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
            Toast.makeText(requireContext(), getString(R.string.export_file_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        val url = if (trimmed.startsWith("http", ignoreCase = true)) {
            trimmed
        } else {
            val base = AppConfig.baseUrl.trimEnd('/')
            val path = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
            "$base$path"
        }
        Toast.makeText(requireContext(), getString(R.string.opening_export_file), Toast.LENGTH_SHORT).show()
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(requireContext(), getString(R.string.no_app_to_open), Toast.LENGTH_SHORT).show()
        }
    }
}
