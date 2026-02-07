package com.snapreceipt.io.ui.receipts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ItemReceiptSelectBinding
import com.snapreceipt.io.domain.model.ReceiptEntity

class ReceiptsSelectableAdapter(
    private var selectedIds: Set<Long>,
    private val onToggle: (Long) -> Unit,
    private val onEditClick: (ReceiptEntity) -> Unit
) : ListAdapter<ReceiptEntity, ReceiptsSelectableAdapter.ViewHolder>(RECEIPT_DIFF) {

    companion object {
        private val RECEIPT_DIFF = object : DiffUtil.ItemCallback<ReceiptEntity>() {
            override fun areItemsTheSame(a: ReceiptEntity, b: ReceiptEntity): Boolean =
                a.receiptId == b.receiptId

            override fun areContentsTheSame(a: ReceiptEntity, b: ReceiptEntity): Boolean = a == b
        }
    }

    fun setReceipts(newReceipts: List<ReceiptEntity>) {
        submitList(newReceipts)
    }

    fun updateSelection(selected: Set<Long>) {
        selectedIds = selected
        notifyItemRangeChanged(0, currentList.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemReceiptSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onToggle, onEditClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val receipt = getItem(position)
        val id = receipt.receiptId
        holder.bind(receipt, id != null && selectedIds.contains(id))
    }

    class ViewHolder(
        private val binding: ItemReceiptSelectBinding,
        private val onToggle: (Long) -> Unit,
        private val onEditClick: (ReceiptEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(receipt: ReceiptEntity, isSelected: Boolean) {
            binding.apply {
                val context = root.context
                selectIcon.isSelected = isSelected
                val receiptId = receipt.receiptId
                selectIcon.setOnClickListener {
                    if (receiptId != null) {
                        onToggle(receiptId)
                    }
                }

                merchantName.text = receipt.merchant.orEmpty()
                amount.text = context.getString(
                    R.string.amount_currency_format,
                    receipt.totalAmount ?: 0.0
                )

                val dateText = receipt.receiptDate?.replace('-', '/').orEmpty()
                val categoryLabel = receipt.categoryName.orEmpty()
                val titleType = receipt.receiptType.orEmpty()
                val metaText = context.getString(
                    R.string.receipt_meta_format,
                    categoryLabel,
                    titleType,
                    dateText
                )
                meta.text = metaText

                receiptCard.setOnClickListener { onEditClick(receipt) }
            }
        }
    }
}
