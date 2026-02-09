package com.skybound.space.base.presentation

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import com.skybound.space.base.R

/**
 * Fullscreen loading dialog that renders a scrim above app content.
 * It is intentionally non-cancelable so network-critical flows stay deterministic.
 */
internal class FullscreenLoadingDialogFragment : DialogFragment() {

    private var loadingMessageView: TextView? = null
    private var pendingMessage: CharSequence? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        pendingMessage = savedInstanceState?.getCharSequence(ARG_MESSAGE)
            ?: arguments?.getCharSequence(ARG_MESSAGE)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar)
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
            messageView.text = ""
            messageView.visibility = View.GONE
            return
        }
        messageView.text = message
        messageView.visibility = View.VISIBLE
    }

    private fun configureWindow() {
        val window = dialog?.window ?: return
        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        // Keep host system bar color/icon appearance unchanged while showing loading.
        WindowCompat.setDecorFitsSystemWindows(window, true)
    }

    companion object {
        private const val ARG_MESSAGE = "arg_message"

        fun newInstance(message: CharSequence?): FullscreenLoadingDialogFragment {
            return FullscreenLoadingDialogFragment().apply {
                arguments = Bundle().apply {
                    putCharSequence(ARG_MESSAGE, message)
                }
            }
        }
    }
}
