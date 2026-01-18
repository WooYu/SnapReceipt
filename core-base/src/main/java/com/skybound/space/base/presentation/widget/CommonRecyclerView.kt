package com.skybound.space.base.presentation.widget

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class CommonRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {
    init {
        clipToPadding = false
    }
}
