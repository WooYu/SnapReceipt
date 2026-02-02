package com.snapreceipt.io.ui.login

import android.app.Dialog
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.skybound.space.base.presentation.BaseFragment
import com.skybound.space.core.util.LogHelper
import com.snapreceipt.io.R
import com.snapreceipt.io.ui.widget.PrimaryActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmailLoginFragment : BaseFragment<LoginViewModel>(R.layout.fragment_email_login) {
    override val viewModel: LoginViewModel by activityViewModels()

    private lateinit var emailInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var getCodeBtn: TextView
    private lateinit var loginBtn: PrimaryActionButton
    private lateinit var emailTab: TextView
    private lateinit var phoneTab: TextView
    private lateinit var agreementCheck: ImageView
    private lateinit var agreementText: TextView
    private lateinit var agreementContainer: View
    private lateinit var backBtn: View
    private var codeLoadingDialog: Dialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        emailInput = view.findViewById(R.id.email_input)
        codeInput = view.findViewById(R.id.code_input)
        getCodeBtn = view.findViewById(R.id.get_code_btn)
        loginBtn = view.findViewById(R.id.login_btn)
        emailTab = view.findViewById(R.id.tab_email)
        phoneTab = view.findViewById(R.id.tab_phone)
        agreementCheck = view.findViewById(R.id.agreement_check)
        agreementText = view.findViewById(R.id.agreement_text)
        agreementContainer = view.findViewById(R.id.agreement_container)
        backBtn = view.findViewById(R.id.back_btn_hot_zone)

        getCodeBtn.setOnClickListener { onGetCodeClick() }
        loginBtn.setOnClickListener { onLoginClick() }
        phoneTab.setOnClickListener { onSwitchLogin() }
        emailTab.setOnClickListener { viewModel.switchToEmail() }
        backBtn.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        agreementCheck.setOnClickListener { toggleAgreement() }
        agreementText.movementMethod = LinkMovementMethod.getInstance()
        agreementText.highlightColor = android.graphics.Color.TRANSPARENT
        agreementContainer.setOnClickListener { toggleAgreement() }
        agreementText.setOnTouchListener { _, event -> handleAgreementTextTouch(event) }
        expandTouchArea(agreementCheck, 16)

        val state = viewModel.uiState.value
        if (emailInput.text.toString() != state.email) {
            emailInput.setText(state.email)
        }
        if (codeInput.text.toString() != state.emailCode) {
            codeInput.setText(state.emailCode)
        }
        emailInput.addTextChangedListener(SimpleTextWatcher { viewModel.updateEmail(it) })
        codeInput.addTextChangedListener(SimpleTextWatcher { viewModel.updateEmailCode(it) })

        observeState()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
    }

    private fun renderState(state: LoginUiState) {
        val canRequestCode = !state.loading && state.codeCountdownSeconds == 0
        getCodeBtn.isEnabled = canRequestCode
        getCodeBtn.text = if (state.codeCountdownSeconds > 0) {
            getString(R.string.login_countdown, state.codeCountdownSeconds)
        } else {
            getString(R.string.login_captcha)
        }
        updateCodeRequestLoading(state.requestingCode)
        loginBtn.isEnabled = !state.loading && state.email.isNotBlank() && state.emailCode.isNotBlank()
        updateTabStyle(state.mode == LoginMode.PHONE)
        updateAgreementState(state.agreementAccepted)
    }

    private fun onGetCodeClick() {
        val email = emailInput.text.toString().trim()
        viewModel.requestCode(email)
    }

    private fun onLoginClick() {
        val email = emailInput.text.toString().trim()
        val code = codeInput.text.toString().trim()
        LogHelper.d(
            "Login",
            "Email login click emailLength=${email.length} codeLength=${code.length}"
        )
        if (!viewModel.uiState.value.agreementAccepted) {
            showAgreementDialog { confirmed ->
                if (confirmed) {
                    viewModel.setAgreementAccepted(true)
                    viewModel.submitEmailLogin(email, code)
                }
            }
            return
        }
        viewModel.submitEmailLogin(email, code)
    }

    private fun onSwitchLogin() {
        viewModel.switchToPhone()
    }

    private fun updateTabStyle(isPhoneSelected: Boolean) {
        if (isPhoneSelected) {
            phoneTab.setTextColor(requireContext().getColor(R.color.text_primary))
            emailTab.setTextColor(requireContext().getColor(R.color.text_secondary))
        } else {
            emailTab.setTextColor(requireContext().getColor(R.color.text_primary))
            phoneTab.setTextColor(requireContext().getColor(R.color.text_secondary))
        }
    }

    private fun updateAgreementState(accepted: Boolean) {
        val drawable = if (accepted) {
            R.drawable.ic_login_checkbox_checked
        } else {
            R.drawable.ic_login_checkbox_unchecked
        }
        agreementCheck.setImageResource(drawable)
        agreementText.text = buildAgreementText()
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

    override fun onDestroyView() {
        codeLoadingDialog?.dismiss()
        codeLoadingDialog = null
        super.onDestroyView()
    }

    private fun toggleAgreement() {
        val newChecked = !viewModel.uiState.value.agreementAccepted
        viewModel.setAgreementAccepted(newChecked)
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

    private fun showAgreementDialog(onResult: (Boolean) -> Unit) {
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

    private class SimpleTextWatcher(
        private val onChanged: (String) -> Unit
    ) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) {
            onChanged(s?.toString()?.trim().orEmpty())
        }
    }

    private fun updateCodeRequestLoading(show: Boolean) {
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

}
