package com.snapreceipt.io.ui.login

import android.os.Bundle
import android.view.View
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.snapreceipt.io.R
import com.skybound.space.base.presentation.BaseFragment
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
    private lateinit var agreementCheck: CheckBox
    private lateinit var agreementText: TextView
    private lateinit var backBtn: ImageView
    private lateinit var toggleCodeVisibility: ImageView
    private var isCodeVisible = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        phoneInput = view.findViewById(R.id.phone_input)
        codeInput = view.findViewById(R.id.code_input)
        getCodeBtn = view.findViewById(R.id.get_code_btn)
        loginBtn = view.findViewById(R.id.login_btn)
        emailTab = view.findViewById(R.id.tab_email)
        phoneTab = view.findViewById(R.id.tab_phone)
        agreementCheck = view.findViewById(R.id.agreement_check)
        agreementText = view.findViewById(R.id.agreement_text)
        backBtn = view.findViewById(R.id.back_btn)
        toggleCodeVisibility = view.findViewById(R.id.toggle_code_visibility)

        getCodeBtn.setOnClickListener { onGetCodeClick() }
        loginBtn.setOnClickListener { onLoginClick() }
        emailTab.setOnClickListener { onSwitchLogin() }
        phoneTab.setOnClickListener { viewModel.switchToPhone() }
        backBtn.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        toggleCodeVisibility.setOnClickListener { toggleCodeVisibility() }
        agreementCheck.setOnCheckedChangeListener { _, checked ->
            viewModel.setAgreementAccepted(checked)
        }

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
        viewModel.submitPhoneLogin(phone, code)
    }

    private fun onSwitchLogin() {
        viewModel.switchToEmail()
    }

    private fun updateTabStyle(isPhoneSelected: Boolean) {
        if (isPhoneSelected) {
            phoneTab.setBackgroundResource(R.drawable.bg_segment_selected)
            phoneTab.setTextColor(requireContext().getColor(R.color.text_primary))
            emailTab.background = null
            emailTab.setTextColor(requireContext().getColor(R.color.text_secondary))
        } else {
            emailTab.setBackgroundResource(R.drawable.bg_segment_selected)
            emailTab.setTextColor(requireContext().getColor(R.color.text_primary))
            phoneTab.background = null
            phoneTab.setTextColor(requireContext().getColor(R.color.text_secondary))
        }
    }

    private fun updateAgreementState(accepted: Boolean) {
        if (agreementCheck.isChecked != accepted) {
            agreementCheck.setOnCheckedChangeListener(null)
            agreementCheck.isChecked = accepted
            agreementCheck.setOnCheckedChangeListener { _, checked ->
                viewModel.setAgreementAccepted(checked)
            }
        }
        agreementText.text = buildAgreementText()
    }

    private fun buildAgreementText(): CharSequence {
        val text = getString(R.string.login_agreement_html)
        val spannable = SpannableString(text)
        val highlightColor = requireContext().getColor(R.color.accent_blue)
        highlightPhrase(spannable, text, "User Agreement", highlightColor)
        highlightPhrase(spannable, text, "Privacy Policy", highlightColor)
        return spannable
    }

    private fun highlightPhrase(
        spannable: SpannableString,
        fullText: String,
        phrase: String,
        color: Int
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
    }

    private fun toggleCodeVisibility() {
        isCodeVisible = !isCodeVisible
        val selection = codeInput.text?.length ?: 0
        codeInput.inputType = if (isCodeVisible) {
            InputType.TYPE_CLASS_NUMBER
        } else {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        toggleCodeVisibility.setImageResource(
            if (isCodeVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
        )
        codeInput.setSelection(selection)
    }
}
