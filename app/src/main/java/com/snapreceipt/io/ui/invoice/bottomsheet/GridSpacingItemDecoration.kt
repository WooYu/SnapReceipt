package com.snapreceipt.io.ui.invoice.bottomsheet

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Grid spacing policy used by category chips.
 * It keeps outer edges flush while preserving internal gaps.
 */
internal class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val horizontalSpacing: Int,
    private val verticalSpacing: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        // Always reset to avoid stale offsets when a recycled view changes row/column.
        outRect.set(0, 0, 0, 0)

        if (spanCount <= 1) {
            if (position > 0) outRect.top = verticalSpacing
            return
        }

        val column = position % spanCount
        val trailingSpacing = horizontalSpacing / 2
        val leadingSpacing = horizontalSpacing - trailingSpacing

        when (column) {
            0 -> {
                outRect.left = 0
                outRect.right = trailingSpacing
            }

            spanCount - 1 -> {
                outRect.left = leadingSpacing
                outRect.right = 0
            }

            else -> {
                outRect.left = leadingSpacing
                outRect.right = trailingSpacing
            }
        }

        if (position >= spanCount) {
            outRect.top = verticalSpacing
        }
    }
}
