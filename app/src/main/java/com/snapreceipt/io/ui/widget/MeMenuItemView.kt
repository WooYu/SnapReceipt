package com.snapreceipt.io.ui.widget

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.snapreceipt.io.R

/**
 * A reusable menu row used in the "Me" page.
 *
 * Supported attrs:
 * - `mmivIcon`: optional left icon; if not set, icon is hidden.
 * - `mmivTitle`: primary text.
 * - `mmivValueText`: optional right text shown before the arrow.
 * - `mmivShowArrow`: whether to show right arrow.
 * - `mmivShowBackground`: whether to keep row background.
 * - `mmivSwapTitleValueStyle`: swap title/value text style.
 *
 * XML example:
 * ```xml
 * <com.snapreceipt.io.ui.widget.MeMenuItemView
 *     android:id="@+id/menu_clear_cache"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:mmivIcon="@drawable/ic_me_settings"
 *     app:mmivTitle="@string/clear_cache"
 *     app:mmivValueText="128 MB"
 *     app:mmivShowArrow="true"
 *     app:mmivShowBackground="true"
 *     app:mmivSwapTitleValueStyle="false" />
 * ```
 *
 * Kotlin example:
 * ```kotlin
 * val item = findViewById<MeMenuItemView>(R.id.menu_clear_cache)
 * item.setTitle(R.string.clear_cache)
 * item.setValueText("0 MB")
 * item.setArrowVisible(true)
 * item.setBackgroundVisible(true)
 * item.setTitleValueStyleSwapped(false)
 * ```
 */
class MeMenuItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val menuIcon: ImageView
    val menuTitle: TextView
    val menuValue: TextView
    val menuArrow: ImageView
    private val valueEndMarginWithArrow: Int
    private val titleDefaultStyle: TextStyleSnapshot
    private val valueDefaultStyle: TextStyleSnapshot
    private var isTitleValueStyleSwapped: Boolean = false
    private var defaultBackgroundDrawable: Drawable? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dpToPx(DEFAULT_MIN_HEIGHT_DP)
        if (background == null) {
            setBackgroundResource(DEFAULT_BACKGROUND_RES_ID)
        }
        // Keep a copy so background can be restored after setBackgroundVisible(false).
        defaultBackgroundDrawable = background?.constantState?.newDrawable()?.mutate()
        val horizontalPadding = dpToPx(DEFAULT_HORIZONTAL_PADDING_DP)
        val verticalPadding = dpToPx(DEFAULT_VERTICAL_PADDING_DP)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        LayoutInflater.from(context).inflate(R.layout.view_me_menu_item_content, this, true)

        menuIcon = findViewById(R.id.menu_icon)
        menuTitle = findViewById(R.id.menu_title)
        menuValue = findViewById(R.id.menu_value)
        menuArrow = findViewById(R.id.menu_arrow)
        valueEndMarginWithArrow = (menuValue.layoutParams as LayoutParams).marginEnd
        titleDefaultStyle = captureTextStyle(menuTitle)
        valueDefaultStyle = captureTextStyle(menuValue)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MeMenuItemView, defStyleAttr, 0)
        val iconDrawable = typedArray.getDrawable(R.styleable.MeMenuItemView_mmivIcon)
        if (iconDrawable != null) {
            menuIcon.setImageDrawable(iconDrawable)
        }
        setIconVisible(iconDrawable != null)
        if (typedArray.hasValue(R.styleable.MeMenuItemView_mmivTitle)) {
            val title = typedArray.getText(R.styleable.MeMenuItemView_mmivTitle)
            setTitle(title)
        }
        setValueText(typedArray.getText(R.styleable.MeMenuItemView_mmivValueText))
        val showArrow = typedArray.getBoolean(R.styleable.MeMenuItemView_mmivShowArrow, true)
        setArrowVisible(showArrow)
        val showBackground = typedArray.getBoolean(R.styleable.MeMenuItemView_mmivShowBackground, true)
        setBackgroundVisible(showBackground)
        val swapTitleValueStyle = typedArray.getBoolean(R.styleable.MeMenuItemView_mmivSwapTitleValueStyle, false)
        setTitleValueStyleSwapped(swapTitleValueStyle)
        typedArray.recycle()
    }

    fun setIcon(@DrawableRes iconRes: Int) {
        menuIcon.setImageResource(iconRes)
        setIconVisible(true)
    }

    fun setTitle(@StringRes titleRes: Int) {
        setTitle(context.getString(titleRes))
    }

    fun setTitle(title: CharSequence?) {
        menuTitle.text = title
        contentDescription = title
    }

    fun setValueText(@StringRes valueRes: Int) {
        setValueText(context.getString(valueRes))
    }

    fun setValueText(value: CharSequence?) {
        // Keep row compact: empty value does not reserve view space.
        val shouldShow = !value.isNullOrBlank()
        menuValue.text = value
        menuValue.visibility = if (shouldShow) VISIBLE else GONE
    }

    fun setArrowVisible(visible: Boolean) {
        // Remove value-end gap when arrow is hidden.
        menuArrow.visibility = if (visible) VISIBLE else GONE
        updateValueEndMargin(visible)
    }

    fun setIconVisible(visible: Boolean) {
        menuIcon.visibility = if (visible) VISIBLE else GONE
    }

    fun setBackgroundVisible(visible: Boolean) {
        if (visible) {
            if (background == null) {
                background = defaultBackgroundDrawable?.constantState?.newDrawable()?.mutate()
            }
        } else {
            background = null
        }
    }

    fun setTitleValueStyleSwapped(swapped: Boolean) {
        if (isTitleValueStyleSwapped == swapped) {
            return
        }
        // Swap full style snapshot (color, text size, typeface/style).
        isTitleValueStyleSwapped = swapped
        if (swapped) {
            applyTextStyle(menuTitle, valueDefaultStyle)
            applyTextStyle(menuValue, titleDefaultStyle)
        } else {
            applyTextStyle(menuTitle, titleDefaultStyle)
            applyTextStyle(menuValue, valueDefaultStyle)
        }
    }

    private fun captureTextStyle(view: TextView): TextStyleSnapshot {
        return TextStyleSnapshot(
            textColor = view.currentTextColor,
            textSizePx = view.textSize,
            typeface = view.typeface,
            typefaceStyle = view.typeface?.style ?: Typeface.NORMAL
        )
    }

    private fun applyTextStyle(view: TextView, style: TextStyleSnapshot) {
        view.setTextColor(style.textColor)
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.textSizePx)
        view.setTypeface(style.typeface, style.typefaceStyle)
    }

    private fun updateValueEndMargin(arrowVisible: Boolean) {
        val layoutParams = menuValue.layoutParams as LayoutParams
        val desiredMarginEnd = if (arrowVisible) valueEndMarginWithArrow else 0
        if (layoutParams.marginEnd != desiredMarginEnd) {
            layoutParams.marginEnd = desiredMarginEnd
            menuValue.layoutParams = layoutParams
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val DEFAULT_MIN_HEIGHT_DP = 52
        private const val DEFAULT_HORIZONTAL_PADDING_DP = 16
        private const val DEFAULT_VERTICAL_PADDING_DP = 14
        @DrawableRes
        private val DEFAULT_BACKGROUND_RES_ID: Int = R.drawable.bg_surface_card_large
    }

    private data class TextStyleSnapshot(
        val textColor: Int,
        val textSizePx: Float,
        val typeface: Typeface?,
        val typefaceStyle: Int
    )
}
