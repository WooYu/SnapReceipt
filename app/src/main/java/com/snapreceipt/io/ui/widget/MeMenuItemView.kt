package com.snapreceipt.io.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.snapreceipt.io.R

class MeMenuItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val menuIcon: ImageView
    val menuTitle: TextView
    val menuArrow: ImageView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dpToPx(DEFAULT_MIN_HEIGHT_DP)
        val horizontalPadding = dpToPx(DEFAULT_HORIZONTAL_PADDING_DP)
        val verticalPadding = dpToPx(DEFAULT_VERTICAL_PADDING_DP)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        LayoutInflater.from(context).inflate(R.layout.view_me_menu_item_content, this, true)

        menuIcon = findViewById(R.id.menu_icon)
        menuTitle = findViewById(R.id.menu_title)
        menuArrow = findViewById(R.id.menu_arrow)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MeMenuItemView, defStyleAttr, 0)
        if (typedArray.hasValue(R.styleable.MeMenuItemView_mmivIcon)) {
            menuIcon.setImageDrawable(typedArray.getDrawable(R.styleable.MeMenuItemView_mmivIcon))
        }
        if (typedArray.hasValue(R.styleable.MeMenuItemView_mmivTitle)) {
            val title = typedArray.getText(R.styleable.MeMenuItemView_mmivTitle)
            setTitle(title)
        }
        val showArrow = typedArray.getBoolean(R.styleable.MeMenuItemView_mmivShowArrow, true)
        menuArrow.visibility = if (showArrow) VISIBLE else GONE
        typedArray.recycle()
    }

    fun setIcon(@DrawableRes iconRes: Int) {
        menuIcon.setImageResource(iconRes)
    }

    fun setTitle(@StringRes titleRes: Int) {
        setTitle(context.getString(titleRes))
    }

    fun setTitle(title: CharSequence?) {
        menuTitle.text = title
        contentDescription = title
    }

    fun setArrowVisible(visible: Boolean) {
        menuArrow.visibility = if (visible) VISIBLE else GONE
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val DEFAULT_MIN_HEIGHT_DP = 52
        private const val DEFAULT_HORIZONTAL_PADDING_DP = 16
        private const val DEFAULT_VERTICAL_PADDING_DP = 14
    }
}
