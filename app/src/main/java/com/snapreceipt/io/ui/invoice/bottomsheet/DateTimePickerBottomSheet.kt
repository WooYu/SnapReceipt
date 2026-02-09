package com.snapreceipt.io.ui.invoice.bottomsheet

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetDatetimeBinding
import com.snapreceipt.io.ui.widget.applyPickerStyle
import com.skybound.space.core.util.DateFormatUtil
import java.util.Calendar

class DateTimePickerBottomSheet(
    initialTime: Long?,
    private val onSelected: (date: String, time: String, display: String) -> Unit
) : BottomSheetDialogFragment() {

    companion object {
        private const val RECENT_YEAR_COUNT = 50
    }

    private val calendar: Calendar = Calendar.getInstance().apply {
        if (initialTime != null) timeInMillis = initialTime
    }

    private var _binding: BottomSheetDatetimeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        _binding = BottomSheetDatetimeBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }
        applyBottomInsets()

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

    private fun applyBottomInsets() {
        val initialBottomPadding = binding.rootContainer.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootContainer) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updatePadding(bottom = initialBottomPadding + bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.rootContainer)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupPickers() {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        val unselectedColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val minYear = currentYear - (RECENT_YEAR_COUNT - 1)
        val maxYear = currentYear
        val clampedYear = calendar.get(Calendar.YEAR).coerceIn(minYear, maxYear)
        calendar.set(Calendar.YEAR, clampedYear)

        binding.pickerYear.minValue = minYear
        binding.pickerYear.maxValue = maxYear
        binding.pickerYear.wrapSelectorWheel = false
        binding.pickerYear.value = clampedYear

        binding.pickerMonth.minValue = 1
        binding.pickerMonth.maxValue = 12
        binding.pickerMonth.wrapSelectorWheel = false
        binding.pickerMonth.value = calendar.get(Calendar.MONTH) + 1

        binding.pickerHour.minValue = 0
        binding.pickerHour.maxValue = 23
        binding.pickerHour.wrapSelectorWheel = false
        binding.pickerHour.value = calendar.get(Calendar.HOUR_OF_DAY)

        binding.pickerMinute.minValue = 0
        binding.pickerMinute.maxValue = 59
        binding.pickerMinute.wrapSelectorWheel = false
        binding.pickerMinute.value = calendar.get(Calendar.MINUTE)

        binding.pickerMonth.setFormatter { value -> value.toString().padStart(2, '0') }
        binding.pickerDay.setFormatter { value -> value.toString().padStart(2, '0') }
        binding.pickerHour.setFormatter { value -> value.toString().padStart(2, '0') }
        binding.pickerMinute.setFormatter { value -> value.toString().padStart(2, '0') }

        binding.pickerYear.applyPickerStyle(
            selectedTextColor = selectedColor,
            unselectedTextColor = unselectedColor,
            dividerColor = Color.TRANSPARENT,
            dividerHeightDp = 0f,
            maxVisibleItemCount = 5
        )
        binding.pickerMonth.applyPickerStyle(
            selectedTextColor = selectedColor,
            unselectedTextColor = unselectedColor,
            dividerColor = Color.TRANSPARENT,
            dividerHeightDp = 0f,
            maxVisibleItemCount = 5
        )
        binding.pickerDay.applyPickerStyle(
            selectedTextColor = selectedColor,
            unselectedTextColor = unselectedColor,
            dividerColor = Color.TRANSPARENT,
            dividerHeightDp = 0f,
            maxVisibleItemCount = 5
        )
        binding.pickerHour.applyPickerStyle(
            selectedTextColor = selectedColor,
            unselectedTextColor = unselectedColor,
            dividerColor = Color.TRANSPARENT,
            dividerHeightDp = 0f,
            maxVisibleItemCount = 5
        )
        binding.pickerMinute.applyPickerStyle(
            selectedTextColor = selectedColor,
            unselectedTextColor = unselectedColor,
            dividerColor = Color.TRANSPARENT,
            dividerHeightDp = 0f,
            maxVisibleItemCount = 5
        )

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
        binding.pickerDay.wrapSelectorWheel = false
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
