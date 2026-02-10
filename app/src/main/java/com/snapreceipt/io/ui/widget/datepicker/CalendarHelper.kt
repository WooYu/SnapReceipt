package com.snapreceipt.io.ui.widget.datepicker

import java.util.Calendar

/**
 * Calendar manipulation helpers for date/time pickers.
 * Encapsulates safe operations to avoid invalid dates (e.g., Feb 30th).
 */

/**
 * Resets time components to midnight (00:00:00.000).
 * Useful when picking only dates (not times).
 */
fun Calendar.resetTimeToMidnight() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

/**
 * Safely updates year, month, and day while handling invalid dates.
 *
 * Steps:
 * 1. Set year and month first
 * 2. Recompute max day for the new year/month (handles leap years, Feb 29, etc.)
 * 3. Clamp the day to the valid range
 * 4. Set the clamped day
 *
 * @return the clamped day value (in case the caller needs to update a day picker)
 */
fun Calendar.setDateSafely(year: Int, monthOneBased: Int, dayOneBased: Int): Int {
    set(Calendar.YEAR, year)
    set(Calendar.MONTH, monthOneBased - 1) // Calendar.MONTH is 0-based
    val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
    val clampedDay = dayOneBased.coerceIn(1, maxDay)
    set(Calendar.DAY_OF_MONTH, clampedDay)
    return clampedDay
}

/**
 * Clamps the calendar's year to the given range.
 * Mutates the calendar in-place if the year is out of bounds.
 */
fun Calendar.clampYear(minYear: Int, maxYear: Int) {
    val currentYear = get(Calendar.YEAR)
    val clamped = currentYear.coerceIn(minYear, maxYear)
    if (clamped != currentYear) {
        set(Calendar.YEAR, clamped)
    }
}

/**
 * Extracts the max day of month for the current year/month setting.
 * Useful for dynamically updating the day picker's range.
 */
fun Calendar.getMaxDayOfMonth(): Int =
    getActualMaximum(Calendar.DAY_OF_MONTH)
