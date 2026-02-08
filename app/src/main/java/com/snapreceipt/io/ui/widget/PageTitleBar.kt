package com.snapreceipt.io.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.snapreceipt.io.R

/**
 * Page header bar with left/right actions and centered title.
 *
 * Supported attrs:
 * - `ptbTitleText`: title text.
 * - `ptbTitleTextColor`: title color.
 * - `ptbLeftIcon`: left icon drawable.
 * - `ptbRightIcon`: right icon drawable.
 * - `ptbShowLeftIcon`: whether to show left icon.
 * - `ptbShowRightIcon`: whether to show right icon.
 * - `ptbBackground`: background drawable or color.
 *
 * XML example:
 * ```xml
 * <com.snapreceipt.io.ui.widget.PageTitleBar
 *     android:id="@+id/page_header"
 *     android:layout_width="0dp"
 *     android:layout_height="wrap_content"
 *     app:layout_constraintStart_toStartOf="parent"
 *     app:layout_constraintEnd_toEndOf="parent"
 *     app:layout_constraintTop_toTopOf="parent"
 *     app:ptbTitleText="@string/settings"
 *     app:ptbShowLeftIcon="true"
 *     app:ptbShowRightIcon="false" />
 * ```
 *
 * Kotlin example:
 * ```kotlin
 * val titleBar = findViewById<PageTitleBar>(R.id.page_header)
 * titleBar.setTitle(R.string.settings)
 * titleBar.setOnLeftIconClickListener { finish() }
 * titleBar.setRightIconVisible(false)
 * ```
 */
class PageTitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val btnBack: ImageView
    val btnAction: ImageView
    val title: TextView

    init {
        // Use minHeight instead of minimumHeight so ConstraintLayout measurement honors it.
        minHeight = dpToPx(DEFAULT_HEIGHT_DP)
        if (background == null) {
            setBackgroundResource(R.drawable.bg_header_gradient)
        }
        LayoutInflater.from(context).inflate(R.layout.view_page_title_bar_content, this, true)

        btnBack = findViewById(R.id.btn_back)
        btnAction = findViewById(R.id.btn_action)
        title = findViewById(R.id.title)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PageTitleBar, defStyleAttr, 0)
        if (typedArray.hasValue(R.styleable.PageTitleBar_ptbBackground)) {
            background = typedArray.getDrawable(R.styleable.PageTitleBar_ptbBackground)
        }
        if (typedArray.hasValue(R.styleable.PageTitleBar_ptbTitleText)) {
            setTitle(typedArray.getText(R.styleable.PageTitleBar_ptbTitleText))
        }
        if (typedArray.hasValue(R.styleable.PageTitleBar_ptbTitleTextColor)) {
            setTitleTextColor(typedArray.getColor(R.styleable.PageTitleBar_ptbTitleTextColor, title.currentTextColor))
        }
        if (typedArray.hasValue(R.styleable.PageTitleBar_ptbLeftIcon)) {
            btnBack.setImageDrawable(typedArray.getDrawable(R.styleable.PageTitleBar_ptbLeftIcon))
        }
        val hasRightIcon = typedArray.hasValue(R.styleable.PageTitleBar_ptbRightIcon)
        if (hasRightIcon) {
            btnAction.setImageDrawable(typedArray.getDrawable(R.styleable.PageTitleBar_ptbRightIcon))
        }

        val showLeft = typedArray.getBoolean(R.styleable.PageTitleBar_ptbShowLeftIcon, true)
        setLeftIconVisible(showLeft)

        val showRight = typedArray.getBoolean(R.styleable.PageTitleBar_ptbShowRightIcon, hasRightIcon)
        setRightIconVisible(showRight)
        typedArray.recycle()
    }

    fun setTitle(@StringRes titleRes: Int) {
        setTitle(context.getString(titleRes))
    }

    fun setTitle(text: CharSequence?) {
        title.text = text
        title.contentDescription = text
    }

    fun setTitleTextColor(@ColorInt color: Int) {
        title.setTextColor(color)
    }

    fun setHeaderBackground(@DrawableRes drawableRes: Int) {
        setBackgroundResource(drawableRes)
    }

    fun setLeftIcon(@DrawableRes drawableRes: Int) {
        btnBack.setImageResource(drawableRes)
        setLeftIconVisible(true)
    }

    fun setRightIcon(@DrawableRes drawableRes: Int) {
        btnAction.setImageResource(drawableRes)
        setRightIconVisible(true)
    }

    fun setLeftIconVisible(visible: Boolean) {
        btnBack.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setRightIconVisible(visible: Boolean) {
        btnAction.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setOnLeftIconClickListener(listener: View.OnClickListener?) {
        btnBack.setOnClickListener(listener)
    }

    fun setOnRightIconClickListener(listener: View.OnClickListener?) {
        btnAction.setOnClickListener(listener)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val DEFAULT_HEIGHT_DP = 88
    }
}
