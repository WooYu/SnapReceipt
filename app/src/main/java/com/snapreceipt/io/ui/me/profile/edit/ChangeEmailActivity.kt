package com.snapreceipt.io.ui.me.profile.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.observeState
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityChangeEmailBinding
import com.snapreceipt.io.util.ContactInputValidator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangeEmailActivity : BaseActivity<ChangeEmailViewModel>() {

    override val viewModel: ChangeEmailViewModel by viewModels()

    private var _binding: ActivityChangeEmailBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityChangeEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pageHeader.setOnLeftIconClickListener { finish() }
        binding.getCodeBtn.setOnClickListener { onGetCodeClick() }
        binding.confirmBtn.setOnClickListener { viewModel.submit() }
        binding.emailInput.doAfterTextChanged { viewModel.updateEmail(it?.toString().orEmpty()) }
        binding.codeInput.doAfterTextChanged { viewModel.updateCode(it?.toString().orEmpty()) }


        observeState(viewModel.uiState) { renderState(it) }
    }

    override fun onDestroy() {
        _binding?.pageHeader?.setOnLeftIconClickListener(null)
        _binding?.getCodeBtn?.setOnClickListener(null)
        _binding?.confirmBtn?.setOnClickListener(null)
        _binding = null
        super.onDestroy()
    }

    private fun renderState(state: ChangeEmailUiState) {
        if (state.loading || state.requestingCode) {
            showGlobalLoading(getString(R.string.loading_please_wait_dynamic))
        } else {
            hideGlobalLoading()
        }
        val countdownSeconds = state.codeCountdownSeconds
        val canRequestCode = !state.loading && !state.requestingCode && countdownSeconds == 0
        binding.getCodeBtn.isEnabled = canRequestCode
        binding.getCodeBtn.text = if (countdownSeconds > 0) {
            getString(R.string.login_countdown, countdownSeconds)
        } else {
            getString(R.string.login_captcha)
        }
        val emailFormatValid = ContactInputValidator.isEmailValid(state.email)
        binding.confirmBtn.isEnabled = !state.loading &&
            emailFormatValid &&
            state.code.trim().isNotBlank()
    }

    private fun onGetCodeClick() {
        val email = binding.emailInput.text.toString().trim()
        if (!ContactInputValidator.isEmailValid(email)) {
            Toast.makeText(this, getString(R.string.email_invalid), Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.requestCode()
    }

    companion object {
        private const val EXTRA_EMAIL = "extra_email"

        fun createIntent(context: Context, currentEmail: String): Intent {
            return Intent(context, ChangeEmailActivity::class.java).apply {
                putExtra(EXTRA_EMAIL, currentEmail)
            }
        }
    }
}
