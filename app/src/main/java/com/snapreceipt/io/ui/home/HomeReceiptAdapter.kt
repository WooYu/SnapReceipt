package com.snapreceipt.io.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ItemReceiptBinding
import com.snapreceipt.io.domain.model.ReceiptEntity
import java.text.SimpleDateFormat
import java.util.Locale

class HomeReceiptAdapter(
    private val onEditClick: (ReceiptEntity) -> Unit
) : RecyclerView.Adapter<HomeReceiptAdapter.ReceiptViewHolder>() {

    private var receipts: List<ReceiptEntity> = emptyList()

    fun setReceipts(newReceipts: List<ReceiptEntity>) {
        receipts = newReceipts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val binding = ItemReceiptBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReceiptViewHolder(binding, onEditClick)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        holder.bind(receipts[position])
    }

    override fun getItemCount(): Int = receipts.size

    class ReceiptViewHolder(
        private val binding: ItemReceiptBinding,
        private val onEditClick: (ReceiptEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(receipt: ReceiptEntity) {
            binding.apply {
                val context = root.context
                merchantName.text = receipt.merchantName
                amount.text = context.getString(R.string.amount_currency_format, receipt.amount)

                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                val metaText = context.getString(
                    R.string.home_receipt_meta_format,
                    receipt.category,
                    receipt.invoiceType,
                    dateFormat.format(java.util.Date(receipt.date))
                )
                meta.text = metaText

                root.setOnClickListener { onEditClick(receipt) }
            }
        }
    }
}
