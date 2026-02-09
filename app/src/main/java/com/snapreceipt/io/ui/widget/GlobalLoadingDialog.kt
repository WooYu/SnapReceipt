package com.snapreceipt.io.ui.widget

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.snapreceipt.io.R

/**
 * Activity-level fullscreen loading overlay that guarantees coverage across
 * content, status bar and navigation bar areas.
 */
class GlobalLoadingDialog(
    private val activity: Activity
) {

    private var overlayView: LoadingOverlayView? = null

    fun show(message: CharSequence?) {
        if (activity.isFinishing || activity.isDestroyed) return
        val overlay = ensureOverlay()
        if (message.isNullOrBlank()) {
            overlay.setText("")
            overlay.show()
        } else {
            overlay.show(message)
        }
        overlay.bringToFront()
    }

    fun show(@StringRes messageRes: Int = R.string.loading) {
        if (activity.isFinishing || activity.isDestroyed) return
        val overlay = ensureOverlay()
        overlay.show(messageRes)
        overlay.bringToFront()
    }

    fun hide() {
        overlayView?.hide()
    }

    fun release() {
        hide()
        removeOverlayFromParent()
        overlayView = null
    }

    private fun ensureOverlay(): LoadingOverlayView {
        overlayView?.let { return it }
        val root = activity.window.decorView as? ViewGroup
            ?: activity.findViewById(android.R.id.content)
        val overlay = LoadingOverlayView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            show(R.string.loading)
            hide()
        }
        root.addView(overlay)
        overlayView = overlay
        return overlay
    }

    private fun removeOverlayFromParent() {
        val overlay = overlayView ?: return
        (overlay.parent as? ViewGroup)?.removeView(overlay)
    }
}
