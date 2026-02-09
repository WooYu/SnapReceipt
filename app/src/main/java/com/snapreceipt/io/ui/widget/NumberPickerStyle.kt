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
    textSizeSp: Float = 16f
) {
    descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
    setBackgroundColor(Color.TRANSPARENT)

    val textSizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        textSizeSp,
        resources.displayMetrics
    )

    updateSelectedEditText(selectedTextColor, textSizePx)
    updateWheelPaint(unselectedTextColor, textSizePx)
    removeSelectionDividers()
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

private fun NumberPicker.removeSelectionDividers() {
    runCatching {
        val dividerField = NumberPicker::class.java.getDeclaredField("mSelectionDivider").apply {
            isAccessible = true
        }
        dividerField.set(this, ColorDrawable(Color.TRANSPARENT))
    }
    runCatching {
        val dividerHeightField = NumberPicker::class.java.getDeclaredField("mSelectionDividerHeight").apply {
            isAccessible = true
        }
        dividerHeightField.setInt(this, 0)
    }
}
