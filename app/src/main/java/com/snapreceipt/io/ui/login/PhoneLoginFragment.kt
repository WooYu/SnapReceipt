package com.snapreceipt.io.ui.login

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.skybound.space.base.presentation.observeState
import com.skybound.space.core.util.LogHelper
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.FragmentPhoneLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhoneLoginFragment : BaseLoginFragment(R.layout.fragment_phone_login) {
    override val viewModel: LoginViewModel by activityViewModels()

    private var _binding: FragmentPhoneLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPhoneLoginBinding.bind(view)
        binding.getCodeBtn.setOnClickListener { onGetCodeClick() }
        binding.loginBtn.setOnClickListener { onLoginClick() }
        binding.tabEmail.setOnClickListener { onSwitchLogin() }
        binding.tabPhone.setOnClickListener { viewModel.switchToPhone() }
        binding.backBtnHotZone.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        bindAgreementViews(binding.root)

        val state = viewModel.uiState.value
        if (binding.phoneInput.text.toString() != state.phone) {
            binding.phoneInput.setText(state.phone)
        }
        if (binding.codeInput.text.toString() != state.phoneCode) {
            binding.codeInput.setText(state.phoneCode)
        }
        binding.phoneInput.addTextChangedListener(SimpleTextWatcher { viewModel.updatePhone(it) })
        binding.codeInput.addTextChangedListener(SimpleTextWatcher { viewModel.updatePhoneCode(it) })

        observeState(viewModel.uiState) { renderState(it) }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            binding.tabPhone.setTextColor(requireContext().getColor(R.color.text_primary))
            binding.tabEmail.setTextColor(requireContext().getColor(R.color.text_secondary))
        } else {
            binding.tabEmail.setTextColor(requireContext().getColor(R.color.text_primary))
            binding.tabPhone.setTextColor(requireContext().getColor(R.color.text_secondary))
        }
    }
}
