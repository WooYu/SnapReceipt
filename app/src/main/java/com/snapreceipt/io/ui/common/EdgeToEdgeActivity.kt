package com.snapreceipt.io.ui.common

import android.graphics.Color
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.snapreceipt.io.R
import com.snapreceipt.io.ui.widget.LoadingOverlayController
import com.skybound.space.base.presentation.LoadingOverlayHost

open class EdgeToEdgeActivity : AppCompatActivity(), LoadingOverlayHost {

    private val loadingOverlayController: LoadingOverlayController by lazy(LazyThreadSafetyMode.NONE) {
        LoadingOverlayController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
    }

    override fun showGlobalLoading(message: CharSequence?) {
        loadingOverlayController.show(message)
    }

    override fun hideGlobalLoading() {
        loadingOverlayController.hide()
    }

    protected fun showLoadingOverlay(show: Boolean, message: CharSequence? = null) {
        if (show) {
            showGlobalLoading(message)
        } else {
            hideGlobalLoading()
        }
    }

    protected fun showLoadingOverlay(@StringRes messageRes: Int = R.string.loading) {
        loadingOverlayController.show(messageRes)
    }

    protected fun hideLoadingOverlay() {
        hideGlobalLoading()
    }

    override fun onDestroy() {
        loadingOverlayController.release()
        super.onDestroy()
    }
}
