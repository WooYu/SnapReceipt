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
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import com.skybound.space.base.presentation.BaseFragment
import com.snapreceipt.io.R

abstract class BaseLoginFragment(@LayoutRes layoutId: Int) : BaseFragment<LoginViewModel>(layoutId) {

    protected lateinit var agreementCheck: ImageView
    protected lateinit var agreementText: TextView
    protected lateinit var agreementContainer: View
    private var codeLoadingDialog: Dialog? = null

    protected fun bindAgreementViews(root: View) {
        agreementCheck = root.findViewById(R.id.agreement_check)
        agreementText = root.findViewById(R.id.agreement_text)
        agreementContainer = root.findViewById(R.id.agreement_container)

        agreementCheck.setOnClickListener { toggleAgreement() }
        agreementText.movementMethod = LinkMovementMethod.getInstance()
        agreementText.highlightColor = android.graphics.Color.TRANSPARENT
        agreementContainer.setOnClickListener { toggleAgreement() }
        agreementText.setOnTouchListener { _, event -> handleAgreementTextTouch(event) }
        expandTouchArea(agreementCheck, 16)
    }

    protected fun updateAgreementState(accepted: Boolean) {
        val drawable = if (accepted) {
            R.drawable.ic_login_checkbox_checked
        } else {
            R.drawable.ic_login_checkbox_unchecked
        }
        agreementCheck.setImageResource(drawable)
        agreementText.text = buildAgreementText()
    }

    protected fun updateCodeRequestLoading(show: Boolean) {
        if (!show) {
            codeLoadingDialog?.dismiss()
            return
        }
        if (!isAdded) return
        val dialog = codeLoadingDialog ?: createCodeLoadingDialog().also { codeLoadingDialog = it }
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    protected fun showAgreementDialog(onResult: (Boolean) -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.agreement_dialog_title)
            .setMessage(R.string.agreement_dialog_message)
            .setPositiveButton(R.string.agreement_dialog_confirm) { dialog, _ ->
                dialog.dismiss()
                onResult(true)
            }
            .setNegativeButton(R.string.agreement_dialog_cancel) { dialog, _ ->
                dialog.dismiss()
                onResult(false)
            }
            .show()
    }

    override fun onDestroyView() {
        codeLoadingDialog?.dismiss()
        codeLoadingDialog = null
        super.onDestroyView()
    }

    protected fun toggleAgreement() {
        val newChecked = !viewModel.uiState.value.agreementAccepted
        viewModel.setAgreementAccepted(newChecked)
    }

    private fun buildAgreementText(): CharSequence {
        val text = getString(R.string.login_agreement_html)
        val spannable = SpannableString(text)
        val highlightColor = requireContext().getColor(R.color.colorPrimary)
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
        val text = agreementText.text
        if (text is Spannable) {
            val x = (event.x - agreementText.totalPaddingLeft + agreementText.scrollX).toInt()
            val y = (event.y - agreementText.totalPaddingTop + agreementText.scrollY).toInt()
            val layout = agreementText.layout
            if (layout != null) {
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())
                val links = text.getSpans(off, off, ClickableSpan::class.java)
                if (links.isNotEmpty()) {
                    return agreementText.movementMethod?.onTouchEvent(agreementText, text, event) ?: false
                }
            }
        }
        if (event.action == MotionEvent.ACTION_UP) {
            toggleAgreement()
        }
        return true
    }

    private fun createCodeLoadingDialog(): Dialog {
        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_code_request_loading)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
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
