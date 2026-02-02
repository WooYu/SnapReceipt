package com.snapreceipt.io.ui.login

import android.app.Dialog
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.TouchDelegate
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.skybound.space.base.presentation.BaseFragment
import com.skybound.space.core.util.LogHelper
import com.snapreceipt.io.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PhoneLoginFragment : BaseFragment<LoginViewModel>(R.layout.fragment_phone_login) {
    override val viewModel: LoginViewModel by activityViewModels()

    private lateinit var phoneInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var getCodeBtn: TextView
    private lateinit var loginBtn: Button
    private lateinit var emailTab: TextView
    private lateinit var phoneTab: TextView
    private lateinit var agreementCheck: ImageView
    private lateinit var agreementText: TextView
    private lateinit var agreementContainer: View
    private lateinit var backBtn: View
    private var codeLoadingDialog: Dialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        phoneInput = view.findViewById(R.id.phone_input)
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
        emailTab.setOnClickListener { onSwitchLogin() }
        phoneTab.setOnClickListener { viewModel.switchToPhone() }
        backBtn.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        agreementCheck.setOnClickListener { toggleAgreement() }
        agreementText.movementMethod = LinkMovementMethod.getInstance()
        agreementText.highlightColor = android.graphics.Color.TRANSPARENT
        agreementContainer.setOnClickListener { toggleAgreement() }
        agreementText.setOnClickListener { toggleAgreement() }
        expandTouchArea(agreementCheck, 16)

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
        loginBtn.isEnabled = !state.loading && state.agreementAccepted
        updateTabStyle(state.mode == LoginMode.PHONE)
        updateAgreementState(state.agreementAccepted)
    }

    private fun onGetCodeClick() {
        val phone = phoneInput.text.toString().trim()
        viewModel.requestCode(phone)
    }

    private fun onLoginClick() {
        val phone = phoneInput.text.toString().trim()
        val code = codeInput.text.toString().trim()
        LogHelper.d(
            "Login",
            "Phone login click phoneLength=${phone.length} codeLength=${code.length}"
        )
        viewModel.submitPhoneLogin(phone, code)
    }

    private fun onSwitchLogin() {
        viewModel.switchToEmail()
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
