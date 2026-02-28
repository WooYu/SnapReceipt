package com.skybound.space.base.presentation

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

/**
 * Shared event dispatch logic used by both [BaseActivity] and [BaseFragment].
 */
object UiEventDispatcher {

    interface Host {
        fun hostContext(): Context
        fun hostRootView(): View?
        fun onNavigate(command: NavigationCommand) {}
        fun onDialog(dialog: UiEvent.Dialog) {}
        fun onSnackbarAction(actionId: String) {}
        fun onCustomEvent(event: UiEvent.Custom) {}
        fun onNavigateBack()
    }

    fun dispatch(host: Host, event: UiEvent) {
        when (event) {
            is UiEvent.Toast -> {
                val text = event.message.ifBlank { event.resId?.let { host.hostContext().getString(it) } ?: "" }
                Toast.makeText(
                    host.hostContext(),
                    text,
                    if (event.long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
            }
            is UiEvent.Snackbar -> {
                val root = host.hostRootView() ?: return
                Snackbar.make(root, event.message, Snackbar.LENGTH_LONG).apply {
                    if (event.actionLabel != null && event.actionId != null) {
                        setAction(event.actionLabel) { host.onSnackbarAction(event.actionId) }
                    }
                }.show()
            }
            is UiEvent.Dialog -> host.onDialog(event)
            is UiEvent.Navigation -> host.onNavigate(event.command)
            UiEvent.NavigateBack -> host.onNavigateBack()
            is UiEvent.Custom -> host.onCustomEvent(event)
        }
    }
}