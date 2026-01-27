package com.snapreceipt.io.ui.receipts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ItemReceiptSelectBinding
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptEntity

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
        val id = receipt.receiptId
        holder.bind(receipt, id != null && selectedIds.contains(id))
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
                val categoryLabel = receipt.categoryId?.let { ReceiptCategory.labelForId(it) }.orEmpty()
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
