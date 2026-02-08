package com.snapreceipt.io.ui.receipts.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.skybound.space.core.util.DateFormatUtil
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetDateRangeBinding
import java.util.Calendar

class DateRangeBottomSheet(
    initialStart: Long?,
    initialEnd: Long?,
    private val onSelected: (start: Long, end: Long) -> Unit
) : BottomSheetDialogFragment() {

    private val startCalendar: Calendar = Calendar.getInstance()
    private val endCalendar: Calendar = Calendar.getInstance()
    private var editingStart = true

    private var _binding: BottomSheetDateRangeBinding? = null
    private val binding get() = _binding!!

    init {
        val now = System.currentTimeMillis()
        startCalendar.timeInMillis = initialStart ?: (now - 7 * 24 * 60 * 60 * 1000L)
        endCalendar.timeInMillis = initialEnd ?: now
        resetTime(startCalendar)
        resetTime(endCalendar)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        _binding = BottomSheetDateRangeBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        setupPickers()
        updateDateViews()
        updateSelection()

        binding.startDate.setOnClickListener {
            editingStart = true
            syncPickersWithCalendar(startCalendar)
            updateSelection()
        }
        binding.endDate.setOnClickListener {
            editingStart = false
            syncPickersWithCalendar(endCalendar)
            updateSelection()
        }

        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.confirmBtn.setOnClickListener {
            val startMillis = startCalendar.timeInMillis
            val endMillis = endCalendar.timeInMillis
            if (startMillis <= endMillis) {
                onSelected(startMillis, endMillis)
            } else {
                onSelected(endMillis, startMillis)
            }
            dismiss()
        }

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupPickers() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        binding.pickerYear.minValue = currentYear - 2
        binding.pickerYear.maxValue = currentYear + 2

        binding.pickerMonth.minValue = 1
        binding.pickerMonth.maxValue = 12

        binding.pickerYear.setFormatter { value -> value.toString() }
        binding.pickerMonth.setFormatter { value -> value.toString().padStart(2, '0') }
        binding.pickerDay.setFormatter { value -> value.toString().padStart(2, '0') }

        syncPickersWithCalendar(startCalendar)

        val listener = android.widget.NumberPicker.OnValueChangeListener { _, _, _ ->
            val target = if (editingStart) startCalendar else endCalendar
            target.set(Calendar.YEAR, binding.pickerYear.value)
            target.set(Calendar.MONTH, binding.pickerMonth.value - 1)
            target.set(Calendar.DAY_OF_MONTH, binding.pickerDay.value)
            updateDayPicker(target)
            target.set(Calendar.DAY_OF_MONTH, binding.pickerDay.value)
            updateDateViews()
        }

        binding.pickerYear.setOnValueChangedListener(listener)
        binding.pickerMonth.setOnValueChangedListener(listener)
        binding.pickerDay.setOnValueChangedListener(listener)
    }

    private fun updateDayPicker(calendar: Calendar) {
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceAtMost(maxDay)
        binding.pickerDay.minValue = 1
        binding.pickerDay.maxValue = maxDay
        binding.pickerDay.value = currentDay
    }

    private fun syncPickersWithCalendar(calendar: Calendar) {
        binding.pickerYear.value = calendar.get(Calendar.YEAR)
        binding.pickerMonth.value = calendar.get(Calendar.MONTH) + 1
        updateDayPicker(calendar)
    }

    private fun updateDateViews() {
        binding.startDate.text = DateFormatUtil.formatDisplayDate(startCalendar.timeInMillis)
        binding.endDate.text = DateFormatUtil.formatDisplayDate(endCalendar.timeInMillis)
    }

    private fun updateSelection() {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.colorSecondary)
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        if (editingStart) {
            binding.startDate.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.startDate.setTextColor(selectedColor)
            binding.endDate.setBackgroundResource(R.drawable.bg_chip_default)
            binding.endDate.setTextColor(defaultColor)
        } else {
            binding.endDate.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.endDate.setTextColor(selectedColor)
            binding.startDate.setBackgroundResource(R.drawable.bg_chip_default)
            binding.startDate.setTextColor(defaultColor)
        }
    }

    private fun resetTime(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }
}
