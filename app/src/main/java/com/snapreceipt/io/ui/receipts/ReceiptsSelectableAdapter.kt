package com.snapreceipt.io.ui.receipts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.databinding.ItemReceiptSelectBinding
import com.snapreceipt.io.domain.model.ReceiptEntity
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptsSelectableAdapter(
    private var selectedIds: Set<Int>,
    private val onToggle: (Int) -> Unit,
    private val onEditClick: (ReceiptEntity) -> Unit
) : RecyclerView.Adapter<ReceiptsSelectableAdapter.ViewHolder>() {

    private var receipts: List<ReceiptEntity> = emptyList()

    fun setReceipts(newReceipts: List<ReceiptEntity>) {
        receipts = newReceipts
        notifyDataSetChanged()
    }

    fun updateSelection(selected: Set<Int>) {
        selectedIds = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReceiptSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onToggle, onEditClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val receipt = receipts[position]
        holder.bind(receipt, selectedIds.contains(receipt.id))
    }

    override fun getItemCount(): Int = receipts.size

    class ViewHolder(
        private val binding: ItemReceiptSelectBinding,
        private val onToggle: (Int) -> Unit,
        private val onEditClick: (ReceiptEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(receipt: ReceiptEntity, isSelected: Boolean) {
            binding.apply {
                checkbox.setOnCheckedChangeListener(null)
                checkbox.isChecked = isSelected
                checkbox.setOnCheckedChangeListener { _, _ -> onToggle(receipt.id) }

                merchantName.text = receipt.merchantName
                amount.text = "$${String.format("%.2f", receipt.amount)}"

                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                val metaText = "${receipt.category} | ${receipt.invoiceType} | ${dateFormat.format(java.util.Date(receipt.date))}"
                meta.text = metaText

                root.setOnClickListener { onEditClick(receipt) }
            }
        }
    }
}
