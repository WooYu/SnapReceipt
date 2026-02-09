package com.skybound.space.base.presentation

import androidx.fragment.app.FragmentManager

/**
 * Small coordinator for the global fullscreen loading dialog.
 * Keeps show/hide calls idempotent and centralizes the fragment tag.
 */
internal class LoadingDialogController(
    private val fragmentManager: FragmentManager
) {

    fun show(message: CharSequence?) {
        val existing = findDialog()
        if (existing != null) {
            existing.updateMessage(message)
            return
        }
        if (fragmentManager.isStateSaved) return
        FullscreenLoadingDialogFragment.newInstance(message).show(fragmentManager, TAG)
    }

    fun hide() {
        findDialog()?.dismissAllowingStateLoss()
    }

    private fun findDialog(): FullscreenLoadingDialogFragment? {
        return fragmentManager.findFragmentByTag(TAG) as? FullscreenLoadingDialogFragment
    }

    companion object {
        private const val TAG = "global_fullscreen_loading_dialog"
    }
}
