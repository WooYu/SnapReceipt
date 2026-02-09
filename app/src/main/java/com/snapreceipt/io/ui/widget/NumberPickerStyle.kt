package com.snapreceipt.io.ui.widget

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.widget.EditText
import android.widget.NumberPicker
import androidx.annotation.ColorInt

internal fun NumberPicker.applyPickerStyle(
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
    updateSelectionDividers(dividerColor, dividerHeightPx)
    updateVisibleItemCount(maxVisibleItemCount)
    invalidate()
}

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

private fun NumberPicker.updateSelectionDividers(
    @ColorInt dividerColor: Int,
    dividerHeightPx: Int
) {
    runCatching {
        val dividerField = NumberPicker::class.java.getDeclaredField("mSelectionDivider").apply {
            isAccessible = true
        }
        dividerField.set(this, ColorDrawable(dividerColor))
    }
    runCatching {
        val dividerHeightField = NumberPicker::class.java.getDeclaredField("mSelectionDividerHeight").apply {
            isAccessible = true
        }
        dividerHeightField.setInt(this, dividerHeightPx)
    }
}

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
