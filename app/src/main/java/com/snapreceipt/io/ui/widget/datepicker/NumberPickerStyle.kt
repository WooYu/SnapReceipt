package com.snapreceipt.io.ui.widget.datepicker

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker
import androidx.annotation.ColorInt

/**
 * Extension to customise the look of a [NumberPicker].
 *
 * Because [NumberPicker] does not expose a public API for its selection
 * dividers before API 29, we have to resort to reflection.  Some OEM
 * firmwares rename the private fields, so we try several known field
 * names.  On API 29+ we use the official [NumberPicker.setSelectionDividerHeight].
 */
fun NumberPicker.applyPickerStyle(
    @ColorInt selectedTextColor: Int,
    @ColorInt unselectedTextColor: Int,
    @ColorInt dividerColor: Int = Color.TRANSPARENT,
    dividerHeightDp: Float = 0f,
    textSizeSp: Float = 16f,
    maxVisibleItemCount: Int = 5
) {
    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
    setBackgroundColor(Color.TRANSPARENT)

    val textSizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        textSizeSp,
        resources.displayMetrics
    )
    val dividerHeightPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dividerHeightDp,
        resources.displayMetrics
    ).toInt()

    updateSelectedEditText(selectedTextColor, textSizePx)
    updateWheelPaint(unselectedTextColor, textSizePx)
    hideSelectionDividers(dividerColor, dividerHeightPx)
    updateVisibleItemCount(maxVisibleItemCount)
    invalidate()
}

// ── Selected-value EditText ───────────────────────────────────

private fun NumberPicker.updateSelectedEditText(
    @ColorInt textColor: Int,
    textSizePx: Float
) {
    for (index in 0 until childCount) {
        val child = getChildAt(index)
        if (child is EditText) {
            child.setTextColor(textColor)
            child.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
            child.isFocusable = false
            child.isFocusableInTouchMode = false
            child.isCursorVisible = false
            return
        }
    }
}

// ── Non-selected wheel paint ──────────────────────────────────

private fun NumberPicker.updateWheelPaint(
    @ColorInt textColor: Int,
    textSizePx: Float
) {
    runCatching {
        val paintField = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint").apply {
            isAccessible = true
        }
        (paintField.get(this) as? Paint)?.apply {
            color = textColor
            textSize = textSizePx
        }
    }
}

// ── Selection dividers (the two horizontal lines) ─────────────

/**
 * Hides (or re-colours) the selection dividers.  Strategy:
 * 1. API 29+: use the official `setSelectionDividerHeight(0)`.
 * 2. Reflection: try known private field names across AOSP & OEM ROMs.
 * 3. Fallback: walk child views and hide any thin horizontal [View]s
 *    that look like dividers.
 */
private fun NumberPicker.hideSelectionDividers(
    @ColorInt dividerColor: Int,
    dividerHeightPx: Int
) {
    // ── Strategy 1 – official API (Android 10+) ──
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        selectionDividerHeight = dividerHeightPx
    }

    // ── Strategy 2 – reflection on known field names ──
    val dividerFieldNames = listOf(
        "mSelectionDivider",        // AOSP stock
        "mDividerDrawable",         // some Samsung ROMs
        "mSelectionDividers"        // some Huawei/Honor ROMs
    )
    for (name in dividerFieldNames) {
        runCatching {
            NumberPicker::class.java.getDeclaredField(name).apply {
                isAccessible = true
                set(this@hideSelectionDividers, ColorDrawable(dividerColor))
            }
        }
    }

    val heightFieldNames = listOf(
        "mSelectionDividerHeight",  // AOSP stock
        "mDividerHeight"            // some Samsung ROMs
    )
    for (name in heightFieldNames) {
        runCatching {
            NumberPicker::class.java.getDeclaredField(name).apply {
                isAccessible = true
                setInt(this@hideSelectionDividers, dividerHeightPx)
            }
        }
    }

    // ── Strategy 3 – brute-force: hide thin child Views that act as dividers ──
    post {
        hideDividerChildViews(this, dividerColor)
    }
}

/**
 * Walk the NumberPicker's child views and hide any plain [View] whose
 * height is <= 3dp (typical divider thickness).  This catches OEM
 * implementations that create the dividers as child views rather than
 * drawing them on the canvas.
 */
private fun hideDividerChildViews(parent: ViewGroup, @ColorInt color: Int) {
    val thresholdPx = (3 * parent.resources.displayMetrics.density).toInt()
    for (i in 0 until parent.childCount) {
        val child = parent.getChildAt(i)
        if (child is EditText) continue
        if (child is ViewGroup) continue
        // A thin, full-width View is almost certainly a divider
        if (child.height in 1..thresholdPx && child.width >= parent.width / 2) {
            child.visibility = View.INVISIBLE
            child.setBackgroundColor(color)
        }
    }
}

// ── Visible item count ────────────────────────────────────────

private fun NumberPicker.updateVisibleItemCount(maxVisibleItemCount: Int) {
    if (maxVisibleItemCount < 3) return
    val sanitizedCount = if (maxVisibleItemCount % 2 == 0) {
        maxVisibleItemCount - 1
    } else {
        maxVisibleItemCount
    }
    runCatching {
        val itemCountField = NumberPicker::class.java.getDeclaredField("mSelectorWheelItemCount").apply {
            isAccessible = true
        }
        if (itemCountField.getInt(this) == sanitizedCount) {
            return@runCatching
        }

        itemCountField.setInt(this, sanitizedCount)

        val indicesField = NumberPicker::class.java.getDeclaredField("mSelectorIndices").apply {
            isAccessible = true
        }
        indicesField.set(this, IntArray(sanitizedCount))

        NumberPicker::class.java.getDeclaredMethod("initializeSelectorWheelIndices").apply {
            isAccessible = true
            invoke(this@updateVisibleItemCount)
        }

        requestLayout()
        invalidate()
    }
}
