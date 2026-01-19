package com.snapreceipt.io.ui.receipts

import android.os.Bundle
import android.view.View
import android.widget.Button
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
import com.snapreceipt.io.ui.home.dialogs.EditReceiptDialog
import com.skybound.space.base.presentation.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceiptsFragment : BaseFragment<ReceiptsViewModel>(R.layout.fragment_receipts) {
    override val viewModel: ReceiptsViewModel by viewModels()

    private lateinit var receiptList: RecyclerView
    private lateinit var emptyState: View
    private lateinit var actionBar: LinearLayout
    private lateinit var filterDateBtn: TextView
    private lateinit var filterTypeBtn: TextView
    private lateinit var exportBtn: TextView
    private lateinit var selectAllBtn: Button
    private lateinit var deleteBtn: Button
    private lateinit var adapter: ReceiptsSelectableAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        receiptList = view.findViewById(R.id.receipt_list)
        emptyState = view.findViewById(R.id.empty_state)
        actionBar = view.findViewById(R.id.action_bar)
        filterDateBtn = view.findViewById(R.id.filter_date_btn)
        filterTypeBtn = view.findViewById(R.id.filter_type_btn)
        exportBtn = view.findViewById(R.id.export_btn)
        selectAllBtn = view.findViewById(R.id.select_all_btn)
        deleteBtn = view.findViewById(R.id.delete_btn)

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
        if (state.empty) {
            emptyState.visibility = View.VISIBLE
            receiptList.visibility = View.GONE
            adapter.setReceipts(emptyList())
        } else {
            emptyState.visibility = View.GONE
            receiptList.visibility = View.VISIBLE
            adapter.setReceipts(state.receipts)
        }
        actionBar.visibility = if (state.selectedIds.isNotEmpty()) View.VISIBLE else View.GONE
        adapter.updateSelection(state.selectedIds)
    }

    private fun setupAdapter() {
        adapter = ReceiptsSelectableAdapter(
            selectedIds = emptySet(),
            onToggle = { id ->
                viewModel.toggleSelection(id)
            },
            onEditClick = { receipt ->
                showEditDialog(receipt)
            }
        )
        receiptList.adapter = adapter
    }

    private fun setupListeners() {
        filterDateBtn.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.filter_date), Toast.LENGTH_SHORT).show()
        }
        filterTypeBtn.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.filter_type), Toast.LENGTH_SHORT).show()
        }
        exportBtn.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.export), Toast.LENGTH_SHORT).show()
        }
        selectAllBtn.setOnClickListener {
            viewModel.selectAll()
        }
        deleteBtn.setOnClickListener {
            viewModel.deleteSelected()
            Toast.makeText(requireContext(), getString(R.string.delete_selected), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDialog(receipt: ReceiptEntity) {
        EditReceiptDialog(receipt) { updatedReceipt ->
            viewModel.updateReceipt(updatedReceipt)
            Toast.makeText(requireContext(), getString(R.string.success), Toast.LENGTH_SHORT).show()
        }.show(parentFragmentManager, "edit_receipt")
    }
}
