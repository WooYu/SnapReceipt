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

    fun setReceipts(newReceipts: List<ReceiptEntity>) {
        submitList(newReceipts)
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
                    R.string.home_receipt_meta_format,
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
