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
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.BottomSheetDatetimeBinding
import com.skybound.space.core.util.DateFormatUtil
import java.util.Calendar

/**
 * Date-time picker bottom sheet (single date + time selection).
 *
 * Features:
 * - Five NumberPickers: Year / Month / Day / Hour / Minute
 * - Auto-adjusts day range based on year/month (handles leap years, etc.)
 * - Returns three strings via callback: API date, API time, display date-time
 *
 * Usage:
 * ```
 * DateTimePickerBottomSheet(initialMillis) { date, time, display ->
 *     // date = "2024-12-10"
 *     // time = "14:30"
 *     // display = "2024/12/10 14:30"
 * }.show(fragmentManager, "datetime")
 * ```
 */
class DateTimePickerBottomSheet(
    initialTime: Long?,
    private val onSelected: (date: String, time: String, display: String) -> Unit
) : BottomSheetDialogFragment() {

    companion object {
        /**
         * Year range: (current - 49) ~ current.
         * Matches [DateRangeBottomSheet] for consistency.
         */
        private const val RECENT_YEAR_COUNT = 50
    }

    private val calendar: Calendar = Calendar.getInstance().apply {
        if (initialTime != null) timeInMillis = initialTime
    }

    private var _binding: BottomSheetDatetimeBinding? = null
    private val binding get() = _binding!!

    // ── Dialog lifecycle ──────────────────────────────────

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
        updateSelectedDateTimeText()
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
     * Configures the five pickers (year, month, day, hour, minute) with app-standard style.
     * Sets initial values from [calendar].
     */
    private fun setupPickers() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val minYear = currentYear - (RECENT_YEAR_COUNT - 1)
        val maxYear = currentYear

        // Clamp calendar year to valid range
        calendar.clampYear(minYear, maxYear)

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
            ),
            binding.pickerHour to NumberPickerConfig(
                minValue = 0,
                maxValue = 23,
                formatter = { it.toString().padStart(2, '0') }
            ),
            binding.pickerMinute to NumberPickerConfig(
                minValue = 0,
                maxValue = 59,
                formatter = { it.toString().padStart(2, '0') }
            )
        ).applyConfigs(requireContext())

        // Set initial values
        binding.pickerYear.value = calendar.get(Calendar.YEAR)
        binding.pickerMonth.value = calendar.get(Calendar.MONTH) + 1
        binding.pickerHour.value = calendar.get(Calendar.HOUR_OF_DAY)
        binding.pickerMinute.value = calendar.get(Calendar.MINUTE)

        // Update day range for the initial month
        updateDayPickerRange()
    }

    /**
     * Updates [binding.pickerDay]'s range to match the max day of [calendar]'s
     * current year/month. Also clamps the current day value.
     */
    private fun updateDayPickerRange() {
        val maxDay = calendar.getMaxDayOfMonth()
        // Read from calendar, not from pickerDay.value (which may be uninitialized)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH).coerceIn(1, maxDay)

        binding.pickerDay.minValue = 1
        binding.pickerDay.maxValue = maxDay
        binding.pickerDay.value = currentDay
    }

    // ── Listeners ──────────────────────────────────────────

    private fun setupListeners() {
        val onPickerValueChanged = NumberPicker.OnValueChangeListener { _, _, _ ->
            onPickerChanged()
        }
        binding.pickerYear.setOnValueChangedListener(onPickerValueChanged)
        binding.pickerMonth.setOnValueChangedListener(onPickerValueChanged)
        binding.pickerDay.setOnValueChangedListener(onPickerValueChanged)
        binding.pickerHour.setOnValueChangedListener(onPickerValueChanged)
        binding.pickerMinute.setOnValueChangedListener(onPickerValueChanged)

        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.confirmBtn.setOnClickListener { onConfirmClicked() }
    }

    /**
     * Handles any picker value change.
     * Writes picker values into [calendar], adjusts day range if needed, updates preview text.
     */
    private fun onPickerChanged() {
        // Safely update date fields (auto-clamps invalid days)
        val clampedDay = calendar.setDateSafely(
            year = binding.pickerYear.value,
            monthOneBased = binding.pickerMonth.value,
            dayOneBased = binding.pickerDay.value
        )

        // Update time fields (hour/minute are always valid)
        calendar.set(Calendar.HOUR_OF_DAY, binding.pickerHour.value)
        calendar.set(Calendar.MINUTE, binding.pickerMinute.value)

        // Refresh day picker range (in case year/month changed)
        updateDayPickerRange()

        // Re-sync day value if it was clamped
        binding.pickerDay.value = clampedDay

        updateSelectedDateTimeText()
    }

    private fun onConfirmClicked() {
        val dateStr = DateFormatUtil.formatApiDate(calendar.timeInMillis)
        val timeStr = DateFormatUtil.formatTime(calendar.timeInMillis)
        val displayStr = DateFormatUtil.formatDisplayDateTime(calendar.timeInMillis)
        onSelected(dateStr, timeStr, displayStr)
        dismiss()
    }

    // ── UI updates ─────────────────────────────────────────

    /**
     * Updates the preview text showing the currently selected date-time.
     */
    private fun updateSelectedDateTimeText() {
        val display = DateFormatUtil.formatDisplayDateTime(calendar.timeInMillis)
        binding.selectedDate.text = getString(R.string.date_prefix, display)
    }
}
