package com.snapreceipt.io.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ItemReceiptBinding
import com.snapreceipt.io.domain.model.ReceiptEntity

class HomeReceiptAdapter(
    private val onEditClick: (ReceiptEntity) -> Unit
) : ListAdapter<ReceiptEntity, HomeReceiptAdapter.ReceiptViewHolder>(RECEIPT_DIFF) {

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
     * 用于扫描成功场景，无缝插入新项
     */
    fun addItemWithAnimation(receipt: ReceiptEntity) {
        val newList = listOf(receipt) + currentList
        submitList(newList) {
            notifyItemInserted(0)
            // 对新插入的项目执行入场动画
            notifyItemRangeChanged(0, 1)
        }
    }

    /**
     * 删除项，带出场动画
     * 用于删除操作后，带动画消除列表项
     */
    fun removeItemWithAnimation(receiptId: Long) {
        val index = currentList.indexOfFirst { it.receiptId == receiptId }
        if (index >= 0) {
            val newList = currentList.filterIndexed { idx, _ -> idx != index }
            submitList(newList) {
                notifyItemRemoved(index)
            }
        }
    }

    /**
     * 更新项，找到则更新，带淡入淡出动画
     * 用于编辑保存场景，平滑更新列表项
     */
    fun updateItemWithAnimation(receipt: ReceiptEntity) {
        val index = currentList.indexOfFirst { it.receiptId == receipt.receiptId }
        if (index >= 0) {
            val newList = currentList.toMutableList().apply {
                set(index, receipt)
            }
            submitList(newList) {
                // 使用 notifyItemChanged 触发 bind，动画由 ItemAnimator 处理
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val binding = ItemReceiptBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReceiptViewHolder(binding, onEditClick)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReceiptViewHolder(
        private val binding: ItemReceiptBinding,
        private val onEditClick: (ReceiptEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(receipt: ReceiptEntity) {
            binding.apply {
                val context = root.context
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

                root.setOnClickListener { onEditClick(receipt) }
            }
        }
    }
}
