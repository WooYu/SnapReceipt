package com.snapreceipt.io.ui.receipts

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.ui.invoice.bottomsheet.InvoiceTypeBottomSheet
import com.snapreceipt.io.ui.invoice.bottomsheet.TitleTypeBottomSheet
import com.snapreceipt.io.ui.receipts.bottomsheet.DateRangeBottomSheet
import com.snapreceipt.io.ui.receipts.dialogs.ExportSuccessDialog
import com.skybound.space.core.config.AppConfig
import com.skybound.space.base.presentation.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceiptsFragment : BaseFragment<ReceiptsViewModel>(R.layout.fragment_receipts) {
    override val viewModel: ReceiptsViewModel by viewModels()

    private lateinit var receiptList: RecyclerView
    private lateinit var emptyState: View
    private lateinit var actionBar: LinearLayout
    private lateinit var toolbarTitle: TextView
    private lateinit var filterDateBtn: TextView
    private lateinit var filterTitleBtn: TextView
    private lateinit var filterTypeBtn: TextView
    private lateinit var exportActionBtn: Button
    private lateinit var selectAllBtn: View
    private lateinit var selectAllIcon: ImageView
    private lateinit var totalAmount: TextView
    private lateinit var exportLoadingOverlay: View
    private lateinit var adapter: ReceiptsSelectableAdapter
    private var filterStartMillis: Long? = null
    private var filterEndMillis: Long? = null
    private var filterTypeLabel: String? = null
    private var filterTitleLabel: String? = null
    private var currentState: ReceiptsUiState = ReceiptsUiState()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        receiptList = view.findViewById(R.id.receipt_list)
        emptyState = view.findViewById(R.id.empty_state)
        actionBar = view.findViewById(R.id.action_bar)
        toolbarTitle = view.findViewById(R.id.toolbar_title)
        filterDateBtn = view.findViewById(R.id.filter_date_btn)
        filterTitleBtn = view.findViewById(R.id.filter_title_btn)
        filterTypeBtn = view.findViewById(R.id.filter_type_btn)
        exportActionBtn = view.findViewById(R.id.export_action_btn)
        selectAllBtn = view.findViewById(R.id.select_all_btn)
        selectAllIcon = view.findViewById(R.id.select_all_icon)
        totalAmount = view.findViewById(R.id.total_amount)
        exportLoadingOverlay = view.findViewById(R.id.export_loading_overlay)

        setupAdapter()
        setupListeners()
        observeState()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadReceipts()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
    }

    private fun renderState(state: ReceiptsUiState) {
        currentState = state
        if (state.empty) {
            emptyState.visibility = View.VISIBLE
            receiptList.visibility = View.GONE
            adapter.setReceipts(emptyList())
        } else {
            emptyState.visibility = View.GONE
            receiptList.visibility = View.VISIBLE
            adapter.setReceipts(state.receipts)
        }
        val selectedCount = state.selectedIds.size
        toolbarTitle.text = if (selectedCount > 0) {
            getString(R.string.selected_count, selectedCount)
        } else {
            getString(R.string.receipts_title)
        }
        actionBar.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
        adapter.updateSelection(state.selectedIds)

        val selectedTotal = state.receipts
            .filter { state.selectedIds.contains(it.id) }
            .sumOf { it.amount }
        totalAmount.text = getString(R.string.amount_currency_format, selectedTotal)

        val allSelected = state.receipts.isNotEmpty() && state.selectedIds.size == state.receipts.size
        selectAllIcon.isSelected = allSelected

        exportLoadingOverlay.visibility = if (state.exporting) View.VISIBLE else View.GONE
        exportActionBtn.isEnabled = !state.exporting
        exportActionBtn.alpha = if (state.exporting) 0.6f else 1f
        selectAllBtn.isEnabled = !state.exporting
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
        receiptList.adapter = adapter
    }

    private fun setupListeners() {
        filterDateBtn.setOnClickListener {
            DateRangeBottomSheet(filterStartMillis, filterEndMillis) { start, end ->
                filterStartMillis = start
                filterEndMillis = end
                filterDateBtn.text = formatDateRange(start, end)
                viewModel.filterByDateRange(start, end)
            }.show(parentFragmentManager, "date_range_picker")
        }
        filterTypeBtn.setOnClickListener {
            val initial = filterTypeLabel ?: filterTypeBtn.text.toString()
            InvoiceTypeBottomSheet.newInstance(initial) { selected ->
                filterTypeLabel = selected
                filterTypeBtn.text = selected
                viewModel.filterByType(selected)
            }.show(parentFragmentManager, "type_filter_picker")
        }
        filterTitleBtn.setOnClickListener {
            val initial = filterTitleLabel ?: filterTitleBtn.text.toString()
            TitleTypeBottomSheet(initial) { selected ->
                val allLabel = getString(R.string.type_all)
                val normalized = if (selected.equals(allLabel, ignoreCase = true)) "" else selected
                filterTitleLabel = selected
                filterTitleBtn.text = selected
                viewModel.filterByTitleType(normalized)
            }.show(parentFragmentManager, "title_filter_picker")
        }
        exportActionBtn.setOnClickListener {
            viewModel.exportSelected()
        }
        selectAllBtn.setOnClickListener {
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
        val intent = android.content.Intent(requireContext(), com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity::class.java).apply {
            putExtra(com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_RECEIPT_ID, receipt.id.toLong())
            putExtra(com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_MERCHANT, receipt.merchantName)
            putExtra(com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_AMOUNT, receipt.amount.toString())
            putExtra(com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_INVOICE_TYPE, receipt.category)
            putExtra(com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_TITLE_TYPE, receipt.invoiceType)
            putExtra(com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_NOTE, receipt.description)
            putExtra(com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_IMAGE_URL, receipt.imagePath)
            val formatted = formatDateTime(receipt.date)
            putExtra(com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_DATE, formatted.first)
            putExtra(com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_TIME, formatted.second)
        }
        startActivity(intent)
    }

    private fun formatDateRange(start: Long, end: Long): String {
        val format = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
        return getString(
            R.string.date_range_format,
            format.format(java.util.Date(start)),
            format.format(java.util.Date(end))
        )
    }

    private fun formatDateTime(timestamp: Long): Pair<String, String> {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(timestamp)) to timeFormat.format(java.util.Date(timestamp))
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
