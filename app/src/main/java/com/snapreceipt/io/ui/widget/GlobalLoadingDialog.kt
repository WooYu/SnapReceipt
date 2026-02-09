package com.snapreceipt.io.ui.widget

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StringRes
import com.snapreceipt.io.R

/**
 * Activity-level fullscreen loading dialog that guarantees overlay coverage,
 * including areas outside FragmentContainerView (e.g. bottom navigation).
 */
class GlobalLoadingDialog(
    private val activity: Activity
) {

    private var dialog: Dialog? = null
    private var overlayView: LoadingOverlayView? = null

    fun show(message: CharSequence?) {
        if (activity.isFinishing || activity.isDestroyed) return
        ensureDialog()
        if (message.isNullOrBlank()) {
            overlayView?.setText("")
            overlayView?.show()
        } else {
            overlayView?.show(message)
        }
        dialog?.takeIf { !it.isShowing }?.show()
    }

    fun show(@StringRes messageRes: Int = R.string.loading) {
        if (activity.isFinishing || activity.isDestroyed) return
        ensureDialog()
        overlayView?.show(messageRes)
        dialog?.takeIf { !it.isShowing }?.show()
    }

    fun hide() {
        overlayView?.hide()
        dialog?.dismiss()
    }

    fun release() {
        hide()
        dialog = null
        overlayView = null
    }

    private fun ensureDialog() {
        if (dialog != null && overlayView != null) return

        val overlay = LoadingOverlayView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            show(R.string.loading)
        }

        val loadingDialog = Dialog(
            activity,
            android.R.style.Theme_Translucent_NoTitleBar_Fullscreen
        ).apply {
            setContentView(overlay)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        overlayView = overlay
        dialog = loadingDialog
    }
}
