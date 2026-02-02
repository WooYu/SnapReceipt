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
class PhoneLoginFragment : BaseLoginFragment(R.layout.fragment_phone_login) {
    override val viewModel: LoginViewModel by activityViewModels()

    private lateinit var phoneInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var getCodeBtn: TextView
    private lateinit var loginBtn: PrimaryActionButton
    private lateinit var emailTab: TextView
    private lateinit var phoneTab: TextView
    private lateinit var backBtn: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        phoneInput = view.findViewById(R.id.phone_input)
        codeInput = view.findViewById(R.id.code_input)
        getCodeBtn = view.findViewById(R.id.get_code_btn)
        loginBtn = view.findViewById(R.id.login_btn)
        emailTab = view.findViewById(R.id.tab_email)
        phoneTab = view.findViewById(R.id.tab_phone)
        backBtn = view.findViewById(R.id.back_btn_hot_zone)

        getCodeBtn.setOnClickListener { onGetCodeClick() }
        loginBtn.setOnClickListener { onLoginClick() }
        emailTab.setOnClickListener { onSwitchLogin() }
        phoneTab.setOnClickListener { viewModel.switchToPhone() }
        backBtn.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        bindAgreementViews(view)

        val state = viewModel.uiState.value
        if (phoneInput.text.toString() != state.phone) {
            phoneInput.setText(state.phone)
        }
        if (codeInput.text.toString() != state.phoneCode) {
            codeInput.setText(state.phoneCode)
        }
        phoneInput.addTextChangedListener(SimpleTextWatcher { viewModel.updatePhone(it) })
        codeInput.addTextChangedListener(SimpleTextWatcher { viewModel.updatePhoneCode(it) })

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
        val countdownSeconds = state.phoneCodeCountdownSeconds
        val canRequestCode = !state.loading && countdownSeconds == 0
        getCodeBtn.isEnabled = canRequestCode
        getCodeBtn.text = if (countdownSeconds > 0) {
            getString(R.string.login_countdown, countdownSeconds)
        } else {
            getString(R.string.login_captcha)
        }
        updateCodeRequestLoading(state.requestingCode)
        loginBtn.isEnabled = !state.loading && state.phone.isNotBlank() && state.phoneCode.isNotBlank()
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
        if (!viewModel.uiState.value.agreementAccepted) {
            showAgreementDialog { confirmed ->
                if (confirmed) {
                    viewModel.setAgreementAccepted(true)
                    viewModel.submitPhoneLogin(phone, code)
                }
            }
            return
        }
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
}
