package com.snapreceipt.io.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.snapreceipt.io.R

/**
 * Primary CTA button with gradient background, ripple, and optional shadow.
 *
 * Supported attrs:
 * - `android:text`: button label.
 * - `pabCornerRadius`: corner radius.
 * - `pabTextSize`: text size.
 * - `pabTextColor`: text color.
 * - `pabRippleColor`: ripple color.
 * - `pabPressedDarkenFactor`: darken factor for pressed state.
 * - `pabGradientStartColor`: gradient start color.
 * - `pabGradientEndColor`: gradient end color.
 * - `pabEnableShadow`: whether shadow/elevation is enabled.
 *
 * XML example:
 * ```xml
 * <com.snapreceipt.io.ui.widget.PrimaryActionButton
 *     android:id="@+id/submit_btn"
 *     android:layout_width="match_parent"
 *     android:layout_height="56dp"
 *     android:text="@string/continue_text"
 *     app:pabCornerRadius="28dp"
 *     app:pabEnableShadow="true" />
 * ```
 *
 * Kotlin example:
 * ```kotlin
 * val submitButton = findViewById<PrimaryActionButton>(R.id.submit_btn)
 * submitButton.setCornerRadiusDp(24f)
 * submitButton.isEnabled = true
 * ```
 */
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
    private var gradientStartColor = ContextCompat.getColor(context, R.color.colorPrimary)
    private var gradientEndColor = ContextCompat.getColor(context, R.color.colorPrimaryGradientEnd)
    private var enableShadow = true

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
        if (typedArray.hasValue(R.styleable.PrimaryActionButton_pabGradientStartColor)) {
            gradientStartColor = typedArray.getColor(
                R.styleable.PrimaryActionButton_pabGradientStartColor,
                gradientStartColor
            )
        }
        if (typedArray.hasValue(R.styleable.PrimaryActionButton_pabGradientEndColor)) {
            gradientEndColor = typedArray.getColor(
                R.styleable.PrimaryActionButton_pabGradientEndColor,
                gradientEndColor
            )
        }
        if (typedArray.hasValue(R.styleable.PrimaryActionButton_pabEnableShadow)) {
            enableShadow = typedArray.getBoolean(
                R.styleable.PrimaryActionButton_pabEnableShadow,
                enableShadow
            )
        }
        typedArray.recycle()

        if (enableShadow) {
            val elevationAttrs = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.elevation))
            val hasElevation = elevationAttrs.hasValue(0)
            elevationAttrs.recycle()
            if (!hasElevation) {
                elevation = dpToPx(DEFAULT_ELEVATION_DP)
            }
        } else {
            stateListAnimator = null
            elevation = 0f
            translationZ = 0f
        }

        cornerRadius = cornerRadiusPx.toInt()
        insetTop = 0
        insetBottom = 0
        gravity = Gravity.CENTER
        isAllCaps = false
        typeface = Typeface.DEFAULT_BOLD
        textAlignment = TEXT_ALIGNMENT_CENTER
        setTextColor(textColor)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
        backgroundTintList = null
        updateBackground()
    }

    fun setCornerRadiusDp(radiusDp: Float) {
        // Rebuild all button states so pressed/disabled shapes keep same radius.
        cornerRadiusPx = dpToPx(radiusDp)
        cornerRadius = cornerRadiusPx.toInt()
        updateBackground()
    }

    fun setCornerRadiusPx(radiusPx: Float) {
        // Rebuild all button states so pressed/disabled shapes keep same radius.
        cornerRadiusPx = radiusPx
        cornerRadius = cornerRadiusPx.toInt()
        updateBackground()
    }

    private fun updateBackground() {
        // Build state drawables in priority order: pressed > disabled > normal.
        val normal = createGradientDrawable(gradientStartColor, gradientEndColor)
        val pressed = createGradientDrawable(
            darkenColor(gradientStartColor, pressedDarkenFactor),
            darkenColor(gradientEndColor, pressedDarkenFactor)
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
            // Ripple needs a rounded mask to match button corners.
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
            GradientDrawable.Orientation.TOP_BOTTOM,
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
