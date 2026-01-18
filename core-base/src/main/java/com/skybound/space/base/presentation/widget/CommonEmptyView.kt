package com.skybound.space.base.presentation.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

class CommonEmptyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val messageView = TextView(context).apply {
        text = "No data"
        gravity = Gravity.CENTER
    }

    init {
        addView(
            messageView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun setMessage(message: CharSequence) {
        messageView.text = message
    }
}
