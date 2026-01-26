package com.snapreceipt.io.ui.receipts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ItemReceiptSelectBinding
import com.snapreceipt.io.domain.model.ReceiptEntity
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptsSelectableAdapter(
    private var selectedIds: Set<Long>,
    private val onToggle: (Long) -> Unit,
    private val onEditClick: (ReceiptEntity) -> Unit
) : RecyclerView.Adapter<ReceiptsSelectableAdapter.ViewHolder>() {

    private var receipts: List<ReceiptEntity> = emptyList()

    fun setReceipts(newReceipts: List<ReceiptEntity>) {
        receipts = newReceipts
        notifyDataSetChanged()
    }

    fun updateSelection(selected: Set<Long>) {
        selectedIds = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemReceiptSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onToggle, onEditClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val receipt = receipts[position]
        holder.bind(receipt, selectedIds.contains(receipt.id))
    }

    override fun getItemCount(): Int = receipts.size

    class ViewHolder(
        private val binding: ItemReceiptSelectBinding,
        private val onToggle: (Long) -> Unit,
        private val onEditClick: (ReceiptEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(receipt: ReceiptEntity, isSelected: Boolean) {
            binding.apply {
                val context = root.context
                selectIcon.isSelected = isSelected
                selectIcon.setOnClickListener { onToggle(receipt.id) }

                merchantName.text = receipt.merchantName
                amount.text = context.getString(R.string.amount_currency_format, receipt.amount)

                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                val category = receipt.category
                val titleType = receipt.invoiceType
                val metaText = context.getString(
                    R.string.receipt_meta_format,
                    category,
                    titleType,
                    dateFormat.format(java.util.Date(receipt.date))
                )
                meta.text = metaText

                receiptCard.setOnClickListener { onEditClick(receipt) }
            }
        }
    }
}
