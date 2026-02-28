package com.skybound.space.base.presentation

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.skybound.space.base.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

abstract class BaseFragment<VM : com.skybound.space.base.presentation.viewmodel.BaseViewModel>(
    @LayoutRes contentLayoutId: Int
) : Fragment(contentLayoutId), UiEventDispatcher.Host {

    protected abstract val viewModel: VM

    private var loadingDialog: Dialog? = null
    private var hostLoadingShown: Boolean = false
    private var loadingMessageAnimationJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvents()
    }

    override fun onDestroyView() {
        hideHostLoadingIfNeeded()
        dismissLoading()
        super.onDestroyView()
    }

    protected fun showLoading(show: Boolean) {
        showLoading(show, null)
    }

    protected fun showLoading(show: Boolean, message: CharSequence?) {
        if (!show) {
            hideHostLoadingIfNeeded()
            dismissLoading()
            return
        }
        if (!isAdded) return
        val host = activity as? LoadingOverlayHost
        if (host != null) {
            host.showGlobalLoading(message)
            hostLoadingShown = true
            dismissLoading()
            return
        }
        hostLoadingShown = false
        val dialog = loadingDialog ?: createLoadingDialog().also { loadingDialog = it }
        updateLoadingMessage(dialog, message)
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    protected open fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    UiEventDispatcher.dispatch(this@BaseFragment, event)
                }
            }
        }
    }

    // -- UiEventDispatcher.Host --

    override fun hostContext(): Context = requireContext()
    override fun hostRootView(): View? = view
    override fun onNavigateBack() { requireActivity().onBackPressedDispatcher.onBackPressed() }
    override fun onNavigate(command: NavigationCommand) {}
    override fun onDialog(dialog: UiEvent.Dialog) {}
    override fun onSnackbarAction(actionId: String) {}
    override fun onCustomEvent(event: UiEvent.Custom) {}

    // -- Loading dialog internals --

    private fun dismissLoading() {
        stopDialogMessageAnimation()
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun hideHostLoadingIfNeeded() {
        if (!hostLoadingShown) return
        (activity as? LoadingOverlayHost)?.hideGlobalLoading()
        hostLoadingShown = false
    }

    private fun updateLoadingMessage(dialog: Dialog, message: CharSequence?) {
        val messageView = dialog.findViewById<TextView>(R.id.loading_message) ?: return
        if (message.isNullOrBlank()) {
            stopDialogMessageAnimation()
            messageView.text = ""
            messageView.visibility = View.GONE
            return
        }
        messageView.visibility = View.VISIBLE
        val text = message.toString()
        if (text.endsWith(ANIMATED_DOTS_SUFFIX)) {
            startDialogMessageAnimation(dialog, text.removeSuffix(ANIMATED_DOTS_SUFFIX).trimEnd())
        } else {
            stopDialogMessageAnimation()
            messageView.text = text
        }
    }

    private fun startDialogMessageAnimation(dialog: Dialog, baseText: String) {
        stopDialogMessageAnimation()
        loadingMessageAnimationJob = lifecycleScope.launch {
            var dots = 1
            while (isActive) {
                val messageView = dialog.findViewById<TextView>(R.id.loading_message) ?: break
                if (!dialog.isShowing) break
                messageView.text = animatedDotsText(baseText, dots)
                dots = if (dots == 3) 1 else dots + 1
                delay(500)
            }
        }
    }

    private fun stopDialogMessageAnimation() {
        loadingMessageAnimationJob?.cancel()
        loadingMessageAnimationJob = null
    }

    private fun createLoadingDialog(): Dialog {
        return Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_fragment_loading)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private fun animatedDotsText(baseText: String, dots: Int): CharSequence {
        val visibleDots = dots.coerceIn(1, 3)
        val fullText = "$baseText..."
        if (visibleDots == 3) return fullText
        val hiddenStart = baseText.length + visibleDots
        return SpannableString(fullText).apply {
            setSpan(
                ForegroundColorSpan(Color.TRANSPARENT),
                hiddenStart,
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    companion object {
        private const val ANIMATED_DOTS_SUFFIX = "..."
    }
}