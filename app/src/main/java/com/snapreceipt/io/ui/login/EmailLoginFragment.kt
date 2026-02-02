package com.snapreceipt.io.ui.login

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.skybound.space.core.util.LogHelper
import com.snapreceipt.io.R
import com.snapreceipt.io.ui.widget.PrimaryActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmailLoginFragment : BaseLoginFragment(R.layout.fragment_email_login) {
    override val viewModel: LoginViewModel by activityViewModels()

    private lateinit var emailInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var getCodeBtn: TextView
    private lateinit var loginBtn: PrimaryActionButton
    private lateinit var emailTab: TextView
    private lateinit var phoneTab: TextView
    private lateinit var backBtn: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        emailInput = view.findViewById(R.id.email_input)
        codeInput = view.findViewById(R.id.code_input)
        getCodeBtn = view.findViewById(R.id.get_code_btn)
        loginBtn = view.findViewById(R.id.login_btn)
        emailTab = view.findViewById(R.id.tab_email)
        phoneTab = view.findViewById(R.id.tab_phone)
        backBtn = view.findViewById(R.id.back_btn_hot_zone)

        getCodeBtn.setOnClickListener { onGetCodeClick() }
        loginBtn.setOnClickListener { onLoginClick() }
        phoneTab.setOnClickListener { onSwitchLogin() }
        emailTab.setOnClickListener { viewModel.switchToEmail() }
        backBtn.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        bindAgreementViews(view)

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
        val countdownSeconds = state.emailCodeCountdownSeconds
        val canRequestCode = !state.loading && countdownSeconds == 0
        getCodeBtn.isEnabled = canRequestCode
        getCodeBtn.text = if (countdownSeconds > 0) {
            getString(R.string.login_countdown, countdownSeconds)
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
}
