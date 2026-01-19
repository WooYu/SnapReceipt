package com.snapreceipt.io.ui.invoice.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DateTimePickerBottomSheet(
    initialTime: Long?,
    private val onSelected: (date: String, time: String, display: String) -> Unit
) : BottomSheetDialogFragment() {

    private val calendar: Calendar = Calendar.getInstance().apply {
        if (initialTime != null) timeInMillis = initialTime
    }

    private lateinit var yearPicker: NumberPicker
    private lateinit var monthPicker: NumberPicker
    private lateinit var dayPicker: NumberPicker
    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var selectedText: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_datetime, null)
        dialog.setContentView(view)

        yearPicker = view.findViewById(R.id.picker_year)
        monthPicker = view.findViewById(R.id.picker_month)
        dayPicker = view.findViewById(R.id.picker_day)
        hourPicker = view.findViewById(R.id.picker_hour)
        minutePicker = view.findViewById(R.id.picker_minute)
        selectedText = view.findViewById(R.id.selected_date)

        setupPickers()
        updateSelectedText()

        view.findViewById<View>(R.id.cancel_btn).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.confirm_btn).setOnClickListener {
            val date = formatDate(calendar.timeInMillis)
            val time = formatTime(calendar.timeInMillis)
            val display = formatDisplay(calendar.timeInMillis)
            onSelected(date, time, display)
            dismiss()
        }

        return dialog
    }

    private fun setupPickers() {
        val currentYear = calendar.get(Calendar.YEAR)
        yearPicker.minValue = currentYear - 2
        yearPicker.maxValue = currentYear + 2
        yearPicker.value = currentYear

        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = calendar.get(Calendar.MONTH) + 1

        hourPicker.minValue = 0
        hourPicker.maxValue = 23
        hourPicker.value = calendar.get(Calendar.HOUR_OF_DAY)

        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        minutePicker.value = calendar.get(Calendar.MINUTE)

        updateDayPicker()

        val listener = NumberPicker.OnValueChangeListener { _, _, _ ->
            calendar.set(Calendar.YEAR, yearPicker.value)
            calendar.set(Calendar.MONTH, monthPicker.value - 1)
            calendar.set(Calendar.DAY_OF_MONTH, dayPicker.value)
            calendar.set(Calendar.HOUR_OF_DAY, hourPicker.value)
            calendar.set(Calendar.MINUTE, minutePicker.value)
            updateDayPicker()
            updateSelectedText()
        }

        yearPicker.setOnValueChangedListener(listener)
        monthPicker.setOnValueChangedListener(listener)
        dayPicker.setOnValueChangedListener(listener)
        hourPicker.setOnValueChangedListener(listener)
        minutePicker.setOnValueChangedListener(listener)
    }

    private fun updateDayPicker() {
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceAtMost(maxDay)
        dayPicker.minValue = 1
        dayPicker.maxValue = maxDay
        dayPicker.value = currentDay
    }

    private fun updateSelectedText() {
        selectedText.text = formatDisplay(calendar.timeInMillis)
    }

    private fun formatDate(timeMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(timeMillis)

    private fun formatTime(timeMillis: Long): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(timeMillis)

    private fun formatDisplay(timeMillis: Long): String =
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(timeMillis)
}
