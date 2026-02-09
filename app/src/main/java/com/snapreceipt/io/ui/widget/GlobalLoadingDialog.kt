package com.snapreceipt.io.ui.widget

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.WindowCompat
import com.snapreceipt.io.R

/**
 * Activity-level fullscreen loading overlay that guarantees coverage across
 * content, status bar and navigation bar areas.
 */
class GlobalLoadingDialog(
    private val activity: Activity
) {

    private val barScrimColor = Color.parseColor("#80000000")
    private var overlayView: LoadingOverlayView? = null
    private var systemBarsOverridden = false
    private var previousStatusBarColor: Int = Color.TRANSPARENT
    private var previousNavigationBarColor: Int = Color.TRANSPARENT
    private var previousLightStatusBars: Boolean = false
    private var previousLightNavigationBars: Boolean = false

    fun show(message: CharSequence?) {
        if (activity.isFinishing || activity.isDestroyed) return
        val overlay = ensureOverlay()
        applySystemBarScrim()
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
        applySystemBarScrim()
        overlay.show(messageRes)
        overlay.bringToFront()
    }

    fun hide() {
        overlayView?.hide()
        restoreSystemBars()
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

    private fun applySystemBarScrim() {
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (!systemBarsOverridden) {
            previousStatusBarColor = window.statusBarColor
            previousNavigationBarColor = window.navigationBarColor
            previousLightStatusBars = controller?.isAppearanceLightStatusBars == true
            previousLightNavigationBars = controller?.isAppearanceLightNavigationBars == true
            systemBarsOverridden = true
        }
        window.statusBarColor = barScrimColor
        window.navigationBarColor = barScrimColor
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
    }

    private fun restoreSystemBars() {
        if (!systemBarsOverridden) return
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        window.statusBarColor = previousStatusBarColor
        window.navigationBarColor = previousNavigationBarColor
        controller?.isAppearanceLightStatusBars = previousLightStatusBars
        controller?.isAppearanceLightNavigationBars = previousLightNavigationBars
        systemBarsOverridden = false
    }
}
