package com.snapreceipt.io.ui.widget

import android.app.Activity
import androidx.annotation.StringRes
import com.snapreceipt.io.R

/**
 * Reusable controller that manages a fullscreen loading overlay per Activity.
 */
class LoadingOverlayController(
    private val activity: Activity
) {
    private val dialog by lazy(LazyThreadSafetyMode.NONE) {
        GlobalLoadingDialog(activity)
    }

    fun show(message: CharSequence?) {
        dialog.show(message)
    }

    fun show(@StringRes messageRes: Int = R.string.loading) {
        dialog.show(messageRes)
    }

    fun hide() {
        dialog.hide()
    }

    fun release() {
        dialog.release()
    }
}
