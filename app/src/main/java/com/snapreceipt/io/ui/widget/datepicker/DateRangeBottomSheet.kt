package com.snapreceipt.io.ui.widget.datepicker

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.NumberPicker
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.skybound.space.core.util.DateFormatUtil
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetDateRangeBinding
import com.snapreceipt.io.ui.widget.bottomsheet.applyHorizontalMargins
import java.util.Calendar

class DateRangeBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_INITIAL_START = "arg_initial_start"
        private const val ARG_INITIAL_END = "arg_initial_end"
        private const val STATE_START_MILLIS = "state_start_millis"
        private const val STATE_END_MILLIS = "state_end_millis"
        private const val STATE_IS_RESET = "state_is_reset"
        private const val RECENT_YEAR_COUNT = 50
        private const val DEFAULT_RANGE_DAYS = 7

        fun newInstance(
            initialStart: Long?,
            initialEnd: Long?,
            onSelected: (start: Long?, end: Long?) -> Unit
        ): DateRangeBottomSheet {
            return DateRangeBottomSheet().apply {
                arguments = bundleOf(
                    ARG_INITIAL_START to (initialStart ?: -1L),
                    ARG_INITIAL_END to (initialEnd ?: -1L)
                )
                this.onSelected = onSelected
            }
        }
    }

    private val startCalendar: Calendar = Calendar.getInstance()
    private val endCalendar: Calendar = Calendar.getInstance()

    private var editingStart = true
    private var isDateReset = false
    private var onSelected: ((start: Long?, end: Long?) -> Unit)? = null

    private var _binding: BottomSheetDateRangeBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val now = System.currentTimeMillis()
        val argStart = arguments?.getLong(ARG_INITIAL_START, -1L) ?: -1L
        val argEnd = arguments?.getLong(ARG_INITIAL_END, -1L) ?: -1L

        if (savedInstanceState != null) {
            val savedStart = savedInstanceState.getLong(STATE_START_MILLIS, -1L)
            val savedEnd = savedInstanceState.getLong(STATE_END_MILLIS, -1L)
            isDateReset = savedInstanceState.getBoolean(STATE_IS_RESET, false)
            if (savedStart > 0 && savedEnd > 0) {
                startCalendar.timeInMillis = savedStart
                endCalendar.timeInMillis = savedEnd
                startCalendar.resetTimeToMidnight()
                endCalendar.resetTimeToMidnight()
            } else {
                initializeRange(argStart, argEnd, now)
            }
        } else {
            initializeRange(argStart, argEnd, now)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        _binding = BottomSheetDateRangeBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.applyHorizontalMargins(resources.getDimensionPixelSize(R.dimen.page_start_margin))
        applyBottomInsets()

        setupPickers()
        updateDateChips()
        updateChipSelection()
        setupListeners()

        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_START_MILLIS, startCalendar.timeInMillis)
        outState.putLong(STATE_END_MILLIS, endCalendar.timeInMillis)
        outState.putBoolean(STATE_IS_RESET, isDateReset)
        super.onSaveInstanceState(outState)
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
        _binding?.startDate?.setOnClickListener(null)
        _binding?.endDate?.setOnClickListener(null)
        _binding?.cancelBtn?.setOnClickListener(null)
        _binding?.confirmBtn?.setOnClickListener(null)
        _binding?.pickerYear?.setOnValueChangedListener(null)
        _binding?.pickerMonth?.setOnValueChangedListener(null)
        _binding?.pickerDay?.setOnValueChangedListener(null)
        _binding?.rootContainer?.let { ViewCompat.setOnApplyWindowInsetsListener(it, null) }
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        onSelected = null
        super.onDestroy()
    }

    private fun setupPickers() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val minYear = currentYear - (RECENT_YEAR_COUNT - 1)
        val maxYear = currentYear

        startCalendar.clampYear(minYear, maxYear)
        endCalendar.clampYear(minYear, maxYear)

        listOf(
            binding.pickerYear to NumberPickerConfig(
                minValue = minYear,
                maxValue = maxYear
            ),
            binding.pickerMonth to NumberPickerConfig(
                minValue = 1,
                maxValue = 12,
                formatter = { it.toString().padStart(2, '0') }
            ),
            binding.pickerDay to NumberPickerConfig(
                minValue = 1,
                maxValue = 31,
                formatter = { it.toString().padStart(2, '0') }
            )
        ).applyConfigs(requireContext())

        syncPickersToCalendar(startCalendar)
    }

    private fun updateDayPickerRange(calendar: Calendar) {
        val maxDay = calendar.getMaxDayOfMonth()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceIn(1, maxDay)

        binding.pickerDay.minValue = 1
        binding.pickerDay.maxValue = maxDay
        binding.pickerDay.value = currentDay
    }

    private fun syncPickersToCalendar(calendar: Calendar) {
        binding.pickerYear.value = calendar.get(Calendar.YEAR)
        binding.pickerMonth.value = calendar.get(Calendar.MONTH) + 1
        updateDayPickerRange(calendar)
    }

    private fun setupListeners() {
        binding.startDate.setOnClickListener {
            editingStart = true
            syncPickersToCalendar(startCalendar)
            updateChipSelection()
        }
        binding.endDate.setOnClickListener {
            editingStart = false
            syncPickersToCalendar(endCalendar)
            updateChipSelection()
        }

        val onPickerValueChanged = NumberPicker.OnValueChangeListener { _, _, _ ->
            isDateReset = false
            onPickerChanged()
        }
        binding.pickerYear.setOnValueChangedListener(onPickerValueChanged)
        binding.pickerMonth.setOnValueChangedListener(onPickerValueChanged)
        binding.pickerDay.setOnValueChangedListener(onPickerValueChanged)

        binding.cancelBtn.text = getString(R.string.reset)
        binding.cancelBtn.setOnClickListener {
            isDateReset = true
            applyDefaultRange()
            editingStart = true
            syncPickersToCalendar(startCalendar)
            updateDateChips()
            updateChipSelection()
        }
        binding.confirmBtn.setOnClickListener { onConfirmClicked() }
    }

    private fun initializeRange(argStart: Long, argEnd: Long, now: Long) {
        if (argStart <= 0L && argEnd <= 0L) {
            applyDefaultRange(now)
            return
        }

        endCalendar.timeInMillis = if (argEnd > 0L) argEnd else now
        endCalendar.resetTimeToMidnight()

        if (argStart > 0L) {
            startCalendar.timeInMillis = argStart
            startCalendar.resetTimeToMidnight()
        } else {
            startCalendar.timeInMillis = endCalendar.timeInMillis
            startCalendar.add(Calendar.DAY_OF_MONTH, -DEFAULT_RANGE_DAYS)
        }
    }

    private fun applyDefaultRange(referenceNowMillis: Long = System.currentTimeMillis()) {
        endCalendar.timeInMillis = referenceNowMillis
        endCalendar.resetTimeToMidnight()
        startCalendar.timeInMillis = endCalendar.timeInMillis
        startCalendar.add(Calendar.DAY_OF_MONTH, -DEFAULT_RANGE_DAYS)
    }

    private fun onPickerChanged() {
        val targetCalendar = if (editingStart) startCalendar else endCalendar

        val clampedDay = targetCalendar.setDateSafely(
            year = binding.pickerYear.value,
            monthOneBased = binding.pickerMonth.value,
            dayOneBased = binding.pickerDay.value
        )

        updateDayPickerRange(targetCalendar)
        binding.pickerDay.value = clampedDay

        updateDateChips()
    }

    private fun onConfirmClicked() {
        if (isDateReset) {
            onSelected?.invoke(null, null)
            dismiss()
            return
        }

        val startMillis = startCalendar.timeInMillis
        val endMillis = endCalendar.timeInMillis
        if (startMillis > endMillis) {
            Toast.makeText(requireContext(), getString(R.string.date_range_invalid), Toast.LENGTH_SHORT).show()
            return
        }

        onSelected?.invoke(startMillis, endMillis)
        dismiss()
    }

    private fun updateDateChips() {
        binding.startDate.text = DateFormatUtil.formatDisplayDate(startCalendar.timeInMillis)
        binding.endDate.text = DateFormatUtil.formatDisplayDate(endCalendar.timeInMillis)
    }

    private fun updateChipSelection() {
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
}
