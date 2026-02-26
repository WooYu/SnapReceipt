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
import com.snapreceipt.io.databinding.ActivityChangePhoneBinding
import com.snapreceipt.io.util.ContactInputValidator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePhoneActivity : BaseActivity<ChangePhoneViewModel>() {

    override val viewModel: ChangePhoneViewModel by viewModels()

    private var _binding: ActivityChangePhoneBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityChangePhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pageHeader.setOnLeftIconClickListener { finish() }
        binding.getCodeBtn.setOnClickListener { onGetCodeClick() }
        binding.confirmBtn.setOnClickListener { viewModel.submit() }
        binding.phoneInput.doAfterTextChanged { viewModel.updatePhone(it?.toString().orEmpty()) }
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

    private fun renderState(state: ChangePhoneUiState) {
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
        val phoneLengthValid = ContactInputValidator.isPhoneLengthValid(state.phone)
        binding.confirmBtn.isEnabled = !state.loading &&
            phoneLengthValid &&
            state.code.trim().isNotBlank()
    }

    private fun onGetCodeClick() {
        val phone = binding.phoneInput.text.toString().trim()
        if (!ContactInputValidator.isPhoneLengthValid(phone)) {
            Toast.makeText(this, getString(R.string.phone_invalid), Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.requestCode()
    }

    companion object {
        private const val EXTRA_PHONE = "extra_phone"

        fun createIntent(context: Context, currentPhone: String): Intent {
            return Intent(context, ChangePhoneActivity::class.java).apply {
                putExtra(EXTRA_PHONE, currentPhone)
            }
        }
    }
}
