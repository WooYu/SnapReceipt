package com.snapreceipt.io.ui.invoice.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetDatetimeBinding
import com.skybound.space.core.util.DateFormatUtil
import java.util.Calendar

class DateTimePickerBottomSheet(
    initialTime: Long?,
    private val onSelected: (date: String, time: String, display: String) -> Unit
) : BottomSheetDialogFragment() {

    private val calendar: Calendar = Calendar.getInstance().apply {
        if (initialTime != null) timeInMillis = initialTime
    }

    private var _binding: BottomSheetDatetimeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        _binding = BottomSheetDatetimeBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        setupPickers()
        updateSelectedText()

        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.confirmBtn.setOnClickListener {
            val date = formatDate(calendar.timeInMillis)
            val time = formatTime(calendar.timeInMillis)
            val display = formatDisplay(calendar.timeInMillis)
            onSelected(date, time, display)
            dismiss()
        }

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupPickers() {
        val currentYear = calendar.get(Calendar.YEAR)
        binding.pickerYear.minValue = currentYear - 2
        binding.pickerYear.maxValue = currentYear + 2
        binding.pickerYear.value = currentYear

        binding.pickerMonth.minValue = 1
        binding.pickerMonth.maxValue = 12
        binding.pickerMonth.value = calendar.get(Calendar.MONTH) + 1

        binding.pickerHour.minValue = 0
        binding.pickerHour.maxValue = 23
        binding.pickerHour.value = calendar.get(Calendar.HOUR_OF_DAY)

        binding.pickerMinute.minValue = 0
        binding.pickerMinute.maxValue = 59
        binding.pickerMinute.value = calendar.get(Calendar.MINUTE)

        binding.pickerMonth.setFormatter { value -> value.toString().padStart(2, '0') }
        binding.pickerDay.setFormatter { value -> value.toString().padStart(2, '0') }
        binding.pickerHour.setFormatter { value -> value.toString().padStart(2, '0') }
        binding.pickerMinute.setFormatter { value -> value.toString().padStart(2, '0') }

        updateDayPicker()

        val listener = android.widget.NumberPicker.OnValueChangeListener { _, _, _ ->
            calendar.set(Calendar.YEAR, binding.pickerYear.value)
            calendar.set(Calendar.MONTH, binding.pickerMonth.value - 1)
            calendar.set(Calendar.DAY_OF_MONTH, binding.pickerDay.value)
            calendar.set(Calendar.HOUR_OF_DAY, binding.pickerHour.value)
            calendar.set(Calendar.MINUTE, binding.pickerMinute.value)
            updateDayPicker()
            calendar.set(Calendar.DAY_OF_MONTH, binding.pickerDay.value)
            updateSelectedText()
        }

        binding.pickerYear.setOnValueChangedListener(listener)
        binding.pickerMonth.setOnValueChangedListener(listener)
        binding.pickerDay.setOnValueChangedListener(listener)
        binding.pickerHour.setOnValueChangedListener(listener)
        binding.pickerMinute.setOnValueChangedListener(listener)
    }

    private fun updateDayPicker() {
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceAtMost(maxDay)
        binding.pickerDay.minValue = 1
        binding.pickerDay.maxValue = maxDay
        binding.pickerDay.value = currentDay
    }

    private fun updateSelectedText() {
        val display = formatDisplay(calendar.timeInMillis)
        binding.selectedDate.text = getString(R.string.date_prefix, display)
    }

    private fun formatDate(timeMillis: Long): String =
        DateFormatUtil.formatApiDate(timeMillis)

    private fun formatTime(timeMillis: Long): String =
        DateFormatUtil.formatTime(timeMillis)

    private fun formatDisplay(timeMillis: Long): String =
        DateFormatUtil.formatDisplayDateTime(timeMillis)
}
