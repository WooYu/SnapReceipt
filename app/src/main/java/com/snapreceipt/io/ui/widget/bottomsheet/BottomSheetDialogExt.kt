package com.snapreceipt.io.ui.widget.bottomsheet

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog

internal fun BottomSheetDialog.applyHorizontalMargins(
    horizontalMarginPx: Int,
    onBottomSheetReady: (View) -> Unit = {}
) {
    setOnShowListener {
        val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return@setOnShowListener

        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        val params = bottomSheet.layoutParams as? ViewGroup.MarginLayoutParams
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.leftMargin = horizontalMarginPx
            params.rightMargin = horizontalMarginPx
            bottomSheet.layoutParams = params
        }

        onBottomSheetReady(bottomSheet)
    }
}
