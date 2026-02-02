package com.snapreceipt.io.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.snapreceipt.io.R

class PrimaryActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialButton(context, attrs, defStyleAttr) {

    private var cornerRadiusPx = dpToPx(DEFAULT_CORNER_RADIUS_DP)
    private var textSizePx = resources.getDimension(R.dimen.text_size_primary)
    private var textColor = ContextCompat.getColor(context, R.color.colorOnPrimary)
    private var rippleColor = adjustAlpha(
        ContextCompat.getColor(context, R.color.colorPrimary),
        DEFAULT_RIPPLE_ALPHA
    )
    private var pressedDarkenFactor = DEFAULT_PRESSED_DARKEN_FACTOR

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PrimaryActionButton, defStyleAttr, 0)
        if (typedArray.hasValue(R.styleable.PrimaryActionButton_pabCornerRadius)) {
            cornerRadiusPx = typedArray.getDimension(R.styleable.PrimaryActionButton_pabCornerRadius, cornerRadiusPx)
        }
        if (typedArray.hasValue(R.styleable.PrimaryActionButton_pabTextSize)) {
            textSizePx = typedArray.getDimension(R.styleable.PrimaryActionButton_pabTextSize, textSizePx)
        }
        if (typedArray.hasValue(R.styleable.PrimaryActionButton_pabTextColor)) {
            textColor = typedArray.getColor(R.styleable.PrimaryActionButton_pabTextColor, textColor)
        }
        if (typedArray.hasValue(R.styleable.PrimaryActionButton_pabRippleColor)) {
            rippleColor = typedArray.getColor(R.styleable.PrimaryActionButton_pabRippleColor, rippleColor)
        }
        if (typedArray.hasValue(R.styleable.PrimaryActionButton_pabPressedDarkenFactor)) {
            pressedDarkenFactor = typedArray.getFloat(
                R.styleable.PrimaryActionButton_pabPressedDarkenFactor,
                pressedDarkenFactor
            ).coerceIn(MIN_PRESSED_FACTOR, MAX_PRESSED_FACTOR)
        }
        typedArray.recycle()

        val elevationAttrs = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.elevation))
        val hasElevation = elevationAttrs.hasValue(0)
        elevationAttrs.recycle()
        if (!hasElevation) {
            elevation = dpToPx(DEFAULT_ELEVATION_DP)
        }

        cornerRadius = cornerRadiusPx.toInt()
        insetTop = 0
        insetBottom = 0
        setTextColor(textColor)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
        backgroundTintList = null
        updateBackground()
    }

    fun setCornerRadiusDp(radiusDp: Float) {
        cornerRadiusPx = dpToPx(radiusDp)
        cornerRadius = cornerRadiusPx.toInt()
        updateBackground()
    }

    fun setCornerRadiusPx(radiusPx: Float) {
        cornerRadiusPx = radiusPx
        cornerRadius = cornerRadiusPx.toInt()
        updateBackground()
    }

    private fun updateBackground() {
        val startColor = ContextCompat.getColor(context, R.color.colorPrimary)
        val endColor = ContextCompat.getColor(context, R.color.colorSecondary)
        val normal = createGradientDrawable(startColor, endColor)
        val pressed = createGradientDrawable(
            darkenColor(startColor, pressedDarkenFactor),
            darkenColor(endColor, pressedDarkenFactor)
        )
        val disabled = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(DISABLED_COLOR)
            cornerRadius = cornerRadiusPx
        }

        val states = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed), pressed)
            addState(intArrayOf(-android.R.attr.state_enabled), disabled)
            addState(intArrayOf(android.R.attr.state_enabled), normal)
        }

        background = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val ripple = ColorStateList.valueOf(rippleColor)
            val mask = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = cornerRadiusPx
            }
            RippleDrawable(ripple, states, mask)
        } else {
            states
        }
    }

    private fun createGradientDrawable(startColor: Int, endColor: Int): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(startColor, endColor)
        ).apply {
            cornerRadius = cornerRadiusPx
        }
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val alpha = Color.alpha(color)
        val red = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val green = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val blue = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, red, green, blue)
    }

    private fun adjustAlpha(color: Int, alpha: Float): Int {
        val newAlpha = (Color.alpha(color) * alpha).toInt().coerceIn(0, 255)
        return Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    companion object {
        private const val DEFAULT_CORNER_RADIUS_DP = 80f
        private const val DEFAULT_PRESSED_DARKEN_FACTOR = 0.78f
        private const val DEFAULT_RIPPLE_ALPHA = 0.32f
        private const val MIN_PRESSED_FACTOR = 0.5f
        private const val MAX_PRESSED_FACTOR = 0.95f
        private const val DISABLED_COLOR = 0xFFB2B9C5.toInt()
        private const val DEFAULT_ELEVATION_DP = 4f
    }
}
