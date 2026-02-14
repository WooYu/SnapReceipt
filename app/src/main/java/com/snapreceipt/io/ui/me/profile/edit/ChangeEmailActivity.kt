package com.snapreceipt.io.ui.me.profile.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.observeState
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityChangeEmailBinding
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
        binding.getCodeBtn.setOnClickListener { viewModel.requestCode() }
        binding.confirmBtn.setOnClickListener { viewModel.submit() }
        binding.emailInput.doAfterTextChanged { viewModel.updateEmail(it?.toString().orEmpty()) }
        binding.codeInput.doAfterTextChanged { viewModel.updateCode(it?.toString().orEmpty()) }

        val initialEmail = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
        binding.emailInput.setText(initialEmail)
        binding.emailInput.setSelection(initialEmail.length)
        viewModel.updateEmail(initialEmail)

        observeState(viewModel.uiState) { renderState(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun renderState(state: ChangeEmailUiState) {
        val countdownSeconds = state.codeCountdownSeconds
        val canRequestCode = !state.loading && !state.requestingCode && countdownSeconds == 0
        binding.getCodeBtn.isEnabled = canRequestCode
        binding.getCodeBtn.text = if (countdownSeconds > 0) {
            getString(R.string.login_countdown, countdownSeconds)
        } else {
            getString(R.string.login_captcha)
        }
        binding.confirmBtn.isEnabled = !state.loading &&
            state.email.trim().isNotBlank() &&
            state.code.trim().isNotBlank()
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
