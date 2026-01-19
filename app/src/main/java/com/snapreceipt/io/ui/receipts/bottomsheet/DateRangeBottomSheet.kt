package com.snapreceipt.io.ui.receipts.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DateRangeBottomSheet(
    initialStart: Long?,
    initialEnd: Long?,
    private val onSelected: (start: Long, end: Long) -> Unit
) : BottomSheetDialogFragment() {

    private val startCalendar: Calendar = Calendar.getInstance()
    private val endCalendar: Calendar = Calendar.getInstance()
    private var editingStart = true

    private lateinit var startDateView: TextView
    private lateinit var endDateView: TextView
    private lateinit var yearPicker: NumberPicker
    private lateinit var monthPicker: NumberPicker
    private lateinit var dayPicker: NumberPicker

    init {
        val now = System.currentTimeMillis()
        startCalendar.timeInMillis = initialStart ?: (now - 7 * 24 * 60 * 60 * 1000L)
        endCalendar.timeInMillis = initialEnd ?: now
        resetTime(startCalendar)
        resetTime(endCalendar)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_date_range, null)
        dialog.setContentView(view)

        startDateView = view.findViewById(R.id.start_date)
        endDateView = view.findViewById(R.id.end_date)
        yearPicker = view.findViewById(R.id.picker_year)
        monthPicker = view.findViewById(R.id.picker_month)
        dayPicker = view.findViewById(R.id.picker_day)

        setupPickers()
        updateDateViews()
        updateSelection()

        startDateView.setOnClickListener {
            editingStart = true
            syncPickersWithCalendar(startCalendar)
            updateSelection()
        }
        endDateView.setOnClickListener {
            editingStart = false
            syncPickersWithCalendar(endCalendar)
            updateSelection()
        }

        view.findViewById<View>(R.id.cancel_btn).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.confirm_btn).setOnClickListener {
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

    private fun setupPickers() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = currentYear - 2
        yearPicker.maxValue = currentYear + 2

        monthPicker.minValue = 1
        monthPicker.maxValue = 12

        yearPicker.setFormatter { value -> value.toString() }
        monthPicker.setFormatter { value -> value.toString().padStart(2, '0') }
        dayPicker.setFormatter { value -> value.toString().padStart(2, '0') }

        syncPickersWithCalendar(startCalendar)

        val listener = NumberPicker.OnValueChangeListener { _, _, _ ->
            val target = if (editingStart) startCalendar else endCalendar
            target.set(Calendar.YEAR, yearPicker.value)
            target.set(Calendar.MONTH, monthPicker.value - 1)
            target.set(Calendar.DAY_OF_MONTH, dayPicker.value)
            updateDayPicker(target)
            target.set(Calendar.DAY_OF_MONTH, dayPicker.value)
            updateDateViews()
        }

        yearPicker.setOnValueChangedListener(listener)
        monthPicker.setOnValueChangedListener(listener)
        dayPicker.setOnValueChangedListener(listener)
    }

    private fun updateDayPicker(calendar: Calendar) {
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceAtMost(maxDay)
        dayPicker.minValue = 1
        dayPicker.maxValue = maxDay
        dayPicker.value = currentDay
    }

    private fun syncPickersWithCalendar(calendar: Calendar) {
        yearPicker.value = calendar.get(Calendar.YEAR)
        monthPicker.value = calendar.get(Calendar.MONTH) + 1
        updateDayPicker(calendar)
    }

    private fun updateDateViews() {
        val format = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        startDateView.text = format.format(startCalendar.timeInMillis)
        endDateView.text = format.format(endCalendar.timeInMillis)
    }

    private fun updateSelection() {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.chip_selected)
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        if (editingStart) {
            startDateView.setBackgroundResource(R.drawable.bg_date_chip_selected)
            startDateView.setTextColor(selectedColor)
            endDateView.setBackgroundResource(R.drawable.bg_date_chip_default)
            endDateView.setTextColor(defaultColor)
        } else {
            endDateView.setBackgroundResource(R.drawable.bg_date_chip_selected)
            endDateView.setTextColor(selectedColor)
            startDateView.setBackgroundResource(R.drawable.bg_date_chip_default)
            startDateView.setTextColor(defaultColor)
        }
    }

    private fun resetTime(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }
}
