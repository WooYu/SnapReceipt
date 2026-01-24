package com.snapreceipt.io.ui.me.export

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ItemExportRecordBinding
import com.snapreceipt.io.domain.model.ExportRecordEntity
import java.text.SimpleDateFormat
import java.util.Locale

class ExportRecordsAdapter(
    private val onRecordClick: (ExportRecordEntity) -> Unit
) : RecyclerView.Adapter<ExportRecordsAdapter.ViewHolder>() {

    private var records: List<ExportRecordEntity> = emptyList()

    fun submitList(newRecords: List<ExportRecordEntity>) {
        records = newRecords
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExportRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position], onRecordClick)
    }

    override fun getItemCount(): Int = records.size

    class ViewHolder(
        private val binding: ItemExportRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: ExportRecordEntity, onRecordClick: (ExportRecordEntity) -> Unit) {
            val context = binding.root.context
            binding.dateRange.text = buildDateRange(record.beginDate, record.endDate)
            binding.receiptCount.text = record.receiptCount.toString()
            binding.titleType.text = record.exportType.ifBlank { context.getString(R.string.placeholder_dash) }
            binding.amount.text = context.getString(R.string.amount_currency_format, record.totalAmount)
            binding.root.setOnClickListener { onRecordClick(record) }
        }

        private fun buildDateRange(begin: String, end: String): String {
            val context = binding.root.context
            val placeholder = context.getString(R.string.placeholder_dash)
            val start = formatDate(begin)
            val finish = formatDate(end)
            return if (start.isBlank() && finish.isBlank()) {
                placeholder
            } else {
                context.getString(
                    R.string.date_range_format,
                    start.ifBlank { placeholder },
                    finish.ifBlank { placeholder }
                )
            }
        }

        private fun formatDate(value: String): String {
            if (value.isBlank()) return ""
            return runCatching {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                formatter.format(parser.parse(value) ?: return value)
            }.getOrDefault(value)
        }
    }
}
