package com.snapreceipt.io.ui.receipts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.data.db.ReceiptEntity
import com.snapreceipt.io.ui.home.dialogs.EditReceiptDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceiptsFragment : Fragment() {
    private val viewModel: ReceiptsViewModel by viewModels()
    private lateinit var receiptList: RecyclerView
    private lateinit var emptyState: FrameLayout
    private lateinit var actionBar: LinearLayout
    private lateinit var filterDateBtn: TextView
    private lateinit var filterTypeBtn: TextView
    private lateinit var exportBtn: TextView
    private lateinit var selectAllBtn: Button
    private lateinit var deleteBtn: Button
    private lateinit var adapter: SelectableReceiptAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_receipts, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        observeData()
    }

    private fun setupAdapter() {
        adapter = SelectableReceiptAdapter(
            selectedIds = emptySet<Int>(),
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
            Toast.makeText(requireContext(), "日期筛选", Toast.LENGTH_SHORT).show()
        }
        filterTypeBtn.setOnClickListener {
            Toast.makeText(requireContext(), "标题筛选", Toast.LENGTH_SHORT).show()
        }
        exportBtn.setOnClickListener {
            Toast.makeText(requireContext(), "类型筛选", Toast.LENGTH_SHORT).show()
        }
        selectAllBtn.setOnClickListener {
            viewModel.selectAll()
        }
        deleteBtn.setOnClickListener {
            viewModel.deleteSelected()
            Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.receipts.collect { receipts ->
                    if (receipts.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                        receiptList.visibility = View.GONE
                        actionBar.visibility = View.GONE
                    } else {
                        emptyState.visibility = View.GONE
                        receiptList.visibility = View.VISIBLE
                        adapter.setReceipts(receipts)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedReceipts.collect { selected ->
                    if (selected.isNotEmpty()) {
                        actionBar.visibility = View.VISIBLE
                    } else {
                        actionBar.visibility = View.GONE
                    }
                    adapter.updateSelection(selected)
                }
            }
        }
    }

    private fun showEditDialog(receipt: ReceiptEntity) {
        EditReceiptDialog(receipt) { updatedReceipt ->
            viewModel.updateReceipt(updatedReceipt)
            Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show()
        }.show(parentFragmentManager, "edit_receipt")
    }
}
