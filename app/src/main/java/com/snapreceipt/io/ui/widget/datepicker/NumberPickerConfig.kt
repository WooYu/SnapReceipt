package com.snapreceipt.io.ui.widget.datepicker

import android.content.Context
import android.graphics.Color
import android.widget.NumberPicker
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.snapreceipt.io.R

/**
 * Centralized [NumberPicker] configuration to reduce code duplication
 * across date/time picker bottom sheets.
 *
 * Encapsulates:
 * - Range (min/max)
 * - Text formatting
 * - Visual style (colors, dividers, visible item count)
 *
 * @param minValue minimum value (inclusive)
 * @param maxValue maximum value (inclusive)
 * @param formatter formats the numeric value to display string (e.g., zero-padding)
 * @param wrapSelectorWheel whether scrolling wraps around (circular scrolling)
 */
data class NumberPickerConfig(
    val minValue: Int,
    val maxValue: Int,
    val formatter: (Int) -> String = { it.toString() },
    val wrapSelectorWheel: Boolean = false
)

/**
 * Applies the given [config] and the app's standard visual style to this [NumberPicker].
 *
 * Visual style:
 * - Selected text: colorPrimary
 * - Unselected text: text_primary
 * - Dividers: hidden (transparent)
 * - Visible items: 5
 *
 * Call this once after initialising the picker.
 */
fun NumberPicker.setupWithConfig(context: Context, config: NumberPickerConfig) {
    minValue = config.minValue
    maxValue = config.maxValue
    wrapSelectorWheel = config.wrapSelectorWheel
    setFormatter(config.formatter)

    val selectedColor = ContextCompat.getColor(context, R.color.colorPrimary)
    val unselectedColor = ContextCompat.getColor(context, R.color.text_primary)

    applyPickerStyle(
        selectedTextColor = selectedColor,
        unselectedTextColor = unselectedColor,
        dividerColor = Color.TRANSPARENT,
        dividerHeightDp = 0f,
        maxVisibleItemCount = 5
    )
}

/**
 * Batch-configures multiple pickers with the same visual style.
 * Useful when setting up year/month/day/hour/minute pickers in one go.
 *
 * Example:
 * ```
 * listOf(
 *     binding.pickerYear to NumberPickerConfig(2000, 2026),
 *     binding.pickerMonth to NumberPickerConfig(1, 12) { it.toString().padStart(2, '0') }
 * ).applyConfigs(requireContext())
 * ```
 */
fun List<Pair<NumberPicker, NumberPickerConfig>>.applyConfigs(context: Context) {
    forEach { (picker, config) ->
        picker.setupWithConfig(context, config)
    }
}
