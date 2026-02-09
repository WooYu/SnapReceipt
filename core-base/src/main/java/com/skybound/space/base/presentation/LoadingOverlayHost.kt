package com.skybound.space.base.presentation

/**
 * Optional host contract for fragments that want to delegate loading UI to
 * an Activity-level fullscreen overlay.
 */
interface LoadingOverlayHost {
    fun showGlobalLoading(message: CharSequence? = null)
    fun hideGlobalLoading()
}
