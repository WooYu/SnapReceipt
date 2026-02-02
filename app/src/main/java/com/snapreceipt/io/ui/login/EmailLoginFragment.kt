package com.snapreceipt.io.ui.login

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.skybound.space.base.presentation.observeState
import com.skybound.space.core.util.LogHelper
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.FragmentEmailLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmailLoginFragment : BaseLoginFragment(R.layout.fragment_email_login) {
    override val viewModel: LoginViewModel by activityViewModels()

    private var _binding: FragmentEmailLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentEmailLoginBinding.bind(view)
        binding.getCodeBtn.setOnClickListener { onGetCodeClick() }
        binding.loginBtn.setOnClickListener { onLoginClick() }
        binding.tabPhone.setOnClickListener { onSwitchLogin() }
        binding.tabEmail.setOnClickListener { viewModel.switchToEmail() }
        binding.backBtnHotZone.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        bindAgreementViews(binding.root)

        val state = viewModel.uiState.value
        if (binding.emailInput.text.toString() != state.email) {
            binding.emailInput.setText(state.email)
        }
        if (binding.codeInput.text.toString() != state.emailCode) {
            binding.codeInput.setText(state.emailCode)
        }
        binding.emailInput.addTextChangedListener(SimpleTextWatcher { viewModel.updateEmail(it) })
        binding.codeInput.addTextChangedListener(SimpleTextWatcher { viewModel.updateEmailCode(it) })

        observeState(viewModel.uiState) { renderState(it) }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderState(state: LoginUiState) {
        val countdownSeconds = state.emailCodeCountdownSeconds
        val canRequestCode = !state.loading && countdownSeconds == 0
        binding.getCodeBtn.isEnabled = canRequestCode
        binding.getCodeBtn.text = if (countdownSeconds > 0) {
            getString(R.string.login_countdown, countdownSeconds)
        } else {
            getString(R.string.login_captcha)
        }
        updateCodeRequestLoading(state.requestingCode)
        binding.loginBtn.isEnabled = !state.loading && state.email.isNotBlank() && state.emailCode.isNotBlank()
        updateTabStyle(state.mode == LoginMode.PHONE)
        updateAgreementState(state.agreementAccepted)
    }

    private fun onGetCodeClick() {
        val email = binding.emailInput.text.toString().trim()
        viewModel.requestCode(email)
    }

    private fun onLoginClick() {
        val email = binding.emailInput.text.toString().trim()
        val code = binding.codeInput.text.toString().trim()
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
            binding.tabPhone.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            binding.tabEmail.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        } else {
            binding.tabEmail.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            binding.tabPhone.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
    }
}
