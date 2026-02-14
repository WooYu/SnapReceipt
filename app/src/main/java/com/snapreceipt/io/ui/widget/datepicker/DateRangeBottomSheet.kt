package com.snapreceipt.io.ui.widget.datepicker

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.skybound.space.core.util.DateFormatUtil
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetDateRangeBinding
import java.util.Calendar

/**
 * Date-range picker bottom sheet (start date → end date).
 *
 * Features:
 * - Dual calendars: user toggles between start/end via chip buttons
 * - Three NumberPickers: Year / Month / Day
 * - Auto-adjusts day range based on selected month (e.g., Feb 28/29, Apr 30)
 * - Result callback returns Unix timestamps (millis) with time set to 00:00:00
 *
 * Usage:
 * ```
 * DateRangeBottomSheet(startMillis, endMillis) { start, end ->
 *     // Handle selected range
 * }.show(fragmentManager, "date_range")
 * ```
 */
class DateRangeBottomSheet(
    initialStart: Long?,
    initialEnd: Long?,
    private val onSelected: (start: Long?, end: Long?) -> Unit
) : BottomSheetDialogFragment() {

    companion object {
        /**
         * Year range: (current - 49) ~ current.
         * Covers 50 recent years for receipt date filtering.
         */
        private const val RECENT_YEAR_COUNT = 50
    }

    // ── Dual calendars ────────────────────────────────────
    private val startCalendar: Calendar = Calendar.getInstance()
    private val endCalendar: Calendar = Calendar.getInstance()

    /**
     * Which calendar is currently being edited via the pickers.
     * Toggled by clicking start/end date chips.
     */
    private var editingStart = true

    private var _binding: BottomSheetDateRangeBinding? = null
    private val binding get() = _binding!!

    init {
        val now = System.currentTimeMillis()
        startCalendar.timeInMillis = initialStart ?: (now - 7 * 24 * 60 * 60 * 1000L)
        endCalendar.timeInMillis = initialEnd ?: now
        startCalendar.resetTimeToMidnight()
        endCalendar.resetTimeToMidnight()
        enforceValidRange()
    }

    // ── Dialog lifecycle ──────────────────────────────────

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        _binding = BottomSheetDateRangeBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }
        applyBottomInsets()

        setupPickers()
        updateDateChips()
        updateChipSelection()
        setupListeners()

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

    // ── NumberPicker setup ────────────────────────────────

    /**
     * Configures the three pickers (year, month, day) with app-standard style.
     * Sets initial value from [startCalendar].
     */
    private fun setupPickers() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val minYear = currentYear - (RECENT_YEAR_COUNT - 1)
        val maxYear = currentYear

        // Clamp both calendars' years to valid range
        startCalendar.clampYear(minYear, maxYear)
        endCalendar.clampYear(minYear, maxYear)

        // Apply config to all pickers in batch
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
                maxValue = 31,  // will be updated dynamically
                formatter = { it.toString().padStart(2, '0') }
            )
        ).applyConfigs(requireContext())

        // Sync picker values with startCalendar
        syncPickersToCalendar(startCalendar)
    }

    /**
     * Updates [binding.pickerDay]'s range to match the max day of the given [calendar]'s
     * current year/month (handles leap years, 30-day months, etc.).
     *
     * Also clamps the day value from calendar if it exceeds the new max.
     */
    private fun updateDayPickerRange(calendar: Calendar) {
        val maxDay = calendar.getMaxDayOfMonth()
        // Read from calendar, not from pickerDay.value (which may be uninitialized)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceIn(1, maxDay)

        binding.pickerDay.minValue = 1
        binding.pickerDay.maxValue = maxDay
        binding.pickerDay.value = currentDay
    }

    /**
     * Syncs the three pickers (year/month/day) to display the given [calendar]'s date.
     */
    private fun syncPickersToCalendar(calendar: Calendar) {
        binding.pickerYear.value = calendar.get(Calendar.YEAR)
        binding.pickerMonth.value = calendar.get(Calendar.MONTH) + 1  // Calendar.MONTH is 0-based
        updateDayPickerRange(calendar)
    }

    // ── Listeners ──────────────────────────────────────────

    private fun setupListeners() {
        // Chip toggles
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

        // Picker value changes
        val onPickerValueChanged = NumberPicker.OnValueChangeListener { _, _, _ ->
            onPickerChanged()
        }
        binding.pickerYear.setOnValueChangedListener(onPickerValueChanged)
        binding.pickerMonth.setOnValueChangedListener(onPickerValueChanged)
        binding.pickerDay.setOnValueChangedListener(onPickerValueChanged)

        // Action buttons
        binding.cancelBtn.text = getString(R.string.reset)
        binding.cancelBtn.setOnClickListener {
            onSelected(null, null)
            dismiss()
        }
        binding.confirmBtn.setOnClickListener { onConfirmClicked() }
    }

    /**
     * Handles any picker value change.
     * Writes picker values into the active calendar (start or end),
     * then refreshes the day picker and chip display.
     */
    private fun onPickerChanged() {
        val targetCalendar = if (editingStart) startCalendar else endCalendar

        // Safely update calendar from pickers (handles invalid days like Feb 30)
        val clampedDay = targetCalendar.setDateSafely(
            year = binding.pickerYear.value,
            monthOneBased = binding.pickerMonth.value,
            dayOneBased = binding.pickerDay.value
        )

        // Update day picker range (e.g., switching from Jan 31 → Feb 28/29)
        updateDayPickerRange(targetCalendar)

        // Re-sync day picker value if it was clamped
        binding.pickerDay.value = clampedDay

        enforceValidRange()
        updateDateChips()
    }

    private fun onConfirmClicked() {
        enforceValidRange()
        val startMillis = startCalendar.timeInMillis
        val endMillis = endCalendar.timeInMillis

        onSelected(startMillis, endMillis)
        dismiss()
    }

    private fun enforceValidRange() {
        val startMillis = startCalendar.timeInMillis
        val endMillis = endCalendar.timeInMillis
        if (startMillis <= endMillis) return

        if (editingStart) {
            endCalendar.timeInMillis = startMillis
            endCalendar.resetTimeToMidnight()
        } else {
            startCalendar.timeInMillis = endMillis
            startCalendar.resetTimeToMidnight()
        }
    }

    // ── UI updates ─────────────────────────────────────────

    /**
     * Updates the two date chips (start/end) to display formatted dates.
     */
    private fun updateDateChips() {
        binding.startDate.text = DateFormatUtil.formatDisplayDate(startCalendar.timeInMillis)
        binding.endDate.text = DateFormatUtil.formatDisplayDate(endCalendar.timeInMillis)
    }

    /**
     * Updates chip visual states (selected chip = highlighted background + colorSecondary text).
     */
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
