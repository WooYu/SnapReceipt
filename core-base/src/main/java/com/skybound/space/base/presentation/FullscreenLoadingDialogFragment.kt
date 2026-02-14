package com.skybound.space.base.presentation

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.skybound.space.base.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Fullscreen loading dialog that renders a scrim above app content.
 * It is intentionally non-cancelable so network-critical flows stay deterministic.
 */
internal class FullscreenLoadingDialogFragment : DialogFragment() {

    private var loadingMessageView: TextView? = null
    private var pendingMessage: CharSequence? = null
    private var messageAnimationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        pendingMessage = savedInstanceState?.getCharSequence(ARG_MESSAGE)
            ?: arguments?.getCharSequence(ARG_MESSAGE)
        setStyle(STYLE_NORMAL, R.style.BaseFullscreenLoadingDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_fullscreen_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingMessageView = view.findViewById(R.id.loading_message)
        renderMessage(pendingMessage)
    }

    override fun onStart() {
        super.onStart()
        configureWindow()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putCharSequence(ARG_MESSAGE, pendingMessage)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        stopMessageAnimation()
        loadingMessageView = null
        super.onDestroyView()
    }

    fun updateMessage(message: CharSequence?) {
        pendingMessage = message
        renderMessage(message)
    }

    private fun renderMessage(message: CharSequence?) {
        val messageView = loadingMessageView ?: return
        if (message.isNullOrBlank()) {
            stopMessageAnimation()
            messageView.text = ""
            messageView.visibility = View.GONE
            return
        }
        messageView.visibility = View.VISIBLE
        val text = message.toString()
        if (text.endsWith(ANIMATED_DOTS_SUFFIX)) {
            startMessageAnimation(text.removeSuffix(ANIMATED_DOTS_SUFFIX).trimEnd())
        } else {
            stopMessageAnimation()
            messageView.text = text
        }
    }

    private fun startMessageAnimation(baseText: String) {
        stopMessageAnimation()
        messageAnimationJob = viewLifecycleOwner.lifecycleScope.launch {
            var dots = 1
            while (isActive) {
                val messageView = loadingMessageView ?: break
                messageView.text = "$baseText${".".repeat(dots)}"
                dots = if (dots == 3) 1 else dots + 1
                delay(500)
            }
        }
    }

    private fun stopMessageAnimation() {
        messageAnimationJob?.cancel()
        messageAnimationJob = null
    }

    private fun configureWindow() {
        val window = dialog?.window ?: return
        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }
        // Cover system bars as well so the loading scrim is truly fullscreen.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    companion object {
        private const val ARG_MESSAGE = "arg_message"
        private const val ANIMATED_DOTS_SUFFIX = "..."

        fun newInstance(message: CharSequence?): FullscreenLoadingDialogFragment {
            return FullscreenLoadingDialogFragment().apply {
                arguments = Bundle().apply {
                    putCharSequence(ARG_MESSAGE, message)
                }
            }
        }
    }
}
