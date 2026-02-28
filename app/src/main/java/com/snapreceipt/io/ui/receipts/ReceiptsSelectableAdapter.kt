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

    /**
     * Updates the receipt list.
     * @param commitCallback optional callback invoked after the list is committed to the adapter.
     */
    fun setReceipts(newReceipts: List<ReceiptEntity>, commitCallback: Runnable? = null) {
        submitList(newReceipts, commitCallback)
    }

    /**
     * 添加项到列表头部，带入场动画
     */
    fun addItemWithAnimation(receipt: ReceiptEntity) {
        val newList = listOf(receipt) + currentList
        submitList(newList) {
            notifyItemInserted(0)
            notifyItemRangeChanged(0, 1)
        }
    }

    /**
     * 删除项，带出场动画
     */
    fun removeItemWithAnimation(receiptId: Long) {
        val index = currentList.indexOfFirst { it.receiptId == receiptId }
        if (index >= 0) {
            val newList = currentList.filterIndexed { idx, _ -> idx != index }
            val wasSelected = selectedIds.contains(receiptId)
            if (wasSelected) {
                selectedIds = selectedIds.minus(receiptId)
            }
            submitList(newList) {
                notifyItemRemoved(index)
            }
        }
    }

    /**
     * 更新项，带淡入淡出动画
     */
    fun updateItemWithAnimation(receipt: ReceiptEntity) {
        val index = currentList.indexOfFirst { it.receiptId == receipt.receiptId }
        if (index >= 0) {
            val newList = currentList.toMutableList().apply {
                set(index, receipt)
            }
            submitList(newList) {
                notifyItemChanged(index)
            }
        }
    }

    fun updateSelection(selected: Set<Long>) {
        if (selectedIds == selected) return
        val previous = selectedIds
        selectedIds = selected
        if (currentList.isEmpty()) return
        currentList.forEachIndexed { index, receipt ->
            val id = receipt.receiptId ?: return@forEachIndexed
            val wasSelected = previous.contains(id)
            val isSelected = selected.contains(id)
            if (wasSelected != isSelected) {
                notifyItemChanged(index)
            }
        }
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
                selectClickArea.setOnClickListener {
                    if (receiptId != null) {
                        onToggle(receiptId)
                    }
                }

                merchantName.text = receipt.merchant.orEmpty()
                amount.text = context.getString(
                    R.string.amount_currency_format,
                    receipt.totalAmount ?: java.math.BigDecimal.ZERO
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
