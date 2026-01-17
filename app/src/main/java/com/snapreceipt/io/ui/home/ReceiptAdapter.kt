package com.snapreceipt.io.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.data.db.ReceiptEntity
import com.snapreceipt.io.databinding.ItemReceiptBinding
import java.text.SimpleDateFormat
import java.util.*

class ReceiptAdapter(
    private val onEditClick: (ReceiptEntity) -> Unit
) : RecyclerView.Adapter<ReceiptAdapter.ReceiptViewHolder>() {

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
                merchantName.text = receipt.merchantName
                amount.text = "$${String.format("%.2f", receipt.amount)}"

                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                val metaText = "${receipt.category} • ${receipt.invoiceType} • ${dateFormat.format(Date(receipt.date))}"
                meta.text = metaText

                root.setOnClickListener { onEditClick(receipt) }
            }
        }
    }
}
