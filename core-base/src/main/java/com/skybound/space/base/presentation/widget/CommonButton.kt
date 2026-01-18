package com.skybound.space.base.presentation.widget

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton

class CommonButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle
) : MaterialButton(context, attrs, defStyleAttr) {
    init {
        isAllCaps = false
    }
}
