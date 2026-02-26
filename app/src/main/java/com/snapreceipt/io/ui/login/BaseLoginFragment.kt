package com.snapreceipt.io.ui.login

import android.app.Dialog
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.view.LayoutInflater
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.skybound.space.base.presentation.BaseFragment
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.DialogAgreementConfirmBinding

abstract class BaseLoginFragment(@LayoutRes layoutId: Int) : BaseFragment<LoginViewModel>(layoutId) {

    // 登录页的 Fragment 通过 activityViewModels() 与 LoginActivity 共享同一 ViewModel，
    // 所有事件已由 LoginActivity (BaseActivity) 统一处理，此处覆写为空以避免重复。
    override fun observeEvents() { /* 由 Activity 统一处理 */ }

    private var agreementCheck: ImageView? = null
    private var agreementText: TextView? = null
    private var agreementContainer: View? = null

    protected fun bindAgreementViews(
        checkView: ImageView,
        textView: TextView,
        containerView: View
    ) {
        agreementCheck = checkView
        agreementText = textView
        agreementContainer = containerView

        checkView.setOnClickListener { toggleAgreement() }
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = android.graphics.Color.TRANSPARENT
        containerView.setOnClickListener { toggleAgreement() }
        textView.setOnTouchListener { _, event -> handleAgreementTextTouch(event) }
        expandTouchArea(checkView, 16)
    }

    protected fun updateAgreementState(accepted: Boolean) {
        val checkView = agreementCheck ?: return
        val textView = agreementText ?: return
        val drawable = if (accepted) {
            R.drawable.ic_login_checkbox_checked
        } else {
            R.drawable.ic_login_checkbox_unchecked
        }
        checkView.setImageResource(drawable)
        textView.text = buildAgreementText()
    }

    protected fun updateCodeRequestLoading(show: Boolean) {
        val message = if (show) getString(R.string.login_requesting_code) else null
        showLoading(show, message)
    }

    protected fun showAgreementDialog(onResult: (Boolean) -> Unit) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogAgreementConfirmBinding.inflate(LayoutInflater.from(requireContext()))
        dialog.setContentView(binding.root)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        binding.agreementDialogTitle.text = getString(R.string.agreement_dialog_title)
        binding.agreementDialogMessage.text = buildAgreementText()
        binding.agreementDialogMessage.movementMethod = LinkMovementMethod.getInstance()
        binding.agreementDialogMessage.highlightColor = android.graphics.Color.TRANSPARENT

        binding.agreementDialogAgree.setOnClickListener {
            dialog.dismiss()
            onResult(true)
        }
        binding.agreementDialogDisagree.setOnClickListener {
            dialog.dismiss()
            onResult(false)
        }

        dialog.show()
    }

    protected fun toggleAgreement() {
        val newChecked = !viewModel.uiState.value.agreementAccepted
        viewModel.setAgreementAccepted(newChecked)
    }

    override fun onDestroyView() {
        agreementCheck?.setOnClickListener(null)
        agreementText?.setOnTouchListener(null)
        agreementContainer?.setOnClickListener(null)
        agreementCheck = null
        agreementText = null
        agreementContainer = null
        super.onDestroyView()
    }

    private fun buildAgreementText(): CharSequence {
        val text = getString(R.string.login_agreement_html)
        val spannable = SpannableString(text)
        val highlightColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        highlightPhrase(
            spannable,
            text,
            getString(R.string.user_agreement),
            highlightColor
        ) { viewModel.openUserAgreement() }
        highlightPhrase(
            spannable,
            text,
            getString(R.string.privacy_policy_label),
            highlightColor
        ) { viewModel.openPrivacyPolicy() }
        return spannable
    }

    private fun highlightPhrase(
        spannable: SpannableString,
        fullText: String,
        phrase: String,
        color: Int,
        onClick: (() -> Unit)? = null
    ) {
        val start = fullText.indexOf(phrase)
        if (start < 0) return
        val end = start + phrase.length
        spannable.setSpan(
            ForegroundColorSpan(color),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (onClick != null) {
            spannable.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onClick()
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = color
                        ds.isUnderlineText = false
                    }
                },
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun handleAgreementTextTouch(event: MotionEvent): Boolean {
        val textView = agreementText ?: return false
        val text = textView.text
        if (text is Spannable) {
            val x = (event.x - textView.totalPaddingLeft + textView.scrollX).toInt()
            val y = (event.y - textView.totalPaddingTop + textView.scrollY).toInt()
            val layout = textView.layout
            if (layout != null) {
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())
                val links = text.getSpans(off, off, ClickableSpan::class.java)
                if (links.isNotEmpty()) {
                    textView.movementMethod?.onTouchEvent(textView, text, event)
                    return true
                }
            }
        }
        if (event.action == MotionEvent.ACTION_UP) {
            toggleAgreement()
        }
        return true
    }

    private fun expandTouchArea(target: View, extraPaddingDp: Int) {
        val parent = target.parent as? View ?: return
        parent.post {
            val rect = Rect()
            target.getHitRect(rect)
            val extra = dpToPx(extraPaddingDp)
            rect.inset(-extra, -extra)
            parent.touchDelegate = TouchDelegate(rect, target)
        }
    }

    private fun dpToPx(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    protected class SimpleTextWatcher(
        private val onChanged: (String) -> Unit
    ) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) {
            onChanged(s?.toString()?.trim().orEmpty())
        }
    }
}