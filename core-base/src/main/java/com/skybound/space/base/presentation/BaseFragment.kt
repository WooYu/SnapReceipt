package com.skybound.space.base.presentation

import android.app.Dialog
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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Fragment 基类：封装事件观察，避免各页面重复收集 SharedFlow。
 */
abstract class BaseFragment<VM : com.skybound.space.base.presentation.viewmodel.BaseViewModel>(
    @LayoutRes contentLayoutId: Int
) : Fragment(contentLayoutId) {

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

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is UiEvent.Toast -> {
                            val text = event.message.ifBlank { event.resId?.let { getString(it) } ?: "" }
                            android.widget.Toast.makeText(
                                requireContext(),
                                text,
                                if (event.long) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        is UiEvent.Snackbar -> {
                            val root = view ?: return@collect
                            Snackbar.make(root, event.message, Snackbar.LENGTH_LONG).apply {
                                if (event.actionLabel != null && event.actionId != null) {
                                    setAction(event.actionLabel) { onSnackbarAction(event.actionId) }
                                }
                            }.show()
                        }
                        is UiEvent.Dialog -> onDialog(event)
                        is UiEvent.Navigation -> onNavigate(event.command)
                        UiEvent.NavigateBack -> requireActivity().onBackPressedDispatcher.onBackPressed()
                        is UiEvent.Custom -> onCustomEvent(event)
                    }
                }
            }
        }
    }

    open fun onNavigate(command: NavigationCommand) {
        // 子类根据实际导航实现处理
    }

    open fun onDialog(dialog: UiEvent.Dialog) {
        // 默认空实现
    }

    open fun onSnackbarAction(actionId: String) {
        // 默认空实现
    }

    open fun onCustomEvent(event: UiEvent.Custom) {
        // 默认空实现
    }

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
