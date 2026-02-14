package com.snapreceipt.io.ui.me.profile.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.observeState
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityChangePhoneBinding
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
        binding.getCodeBtn.setOnClickListener { viewModel.requestCode() }
        binding.confirmBtn.setOnClickListener { viewModel.submit() }
        binding.phoneInput.doAfterTextChanged { viewModel.updatePhone(it?.toString().orEmpty()) }
        binding.codeInput.doAfterTextChanged { viewModel.updateCode(it?.toString().orEmpty()) }

        val initialPhone = intent.getStringExtra(EXTRA_PHONE).orEmpty()
        binding.phoneInput.setText(initialPhone)
        binding.phoneInput.setSelection(initialPhone.length)
        viewModel.updatePhone(initialPhone)

        observeState(viewModel.uiState) { renderState(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun renderState(state: ChangePhoneUiState) {
        val countdownSeconds = state.codeCountdownSeconds
        val canRequestCode = !state.loading && !state.requestingCode && countdownSeconds == 0
        binding.getCodeBtn.isEnabled = canRequestCode
        binding.getCodeBtn.text = if (countdownSeconds > 0) {
            getString(R.string.login_countdown, countdownSeconds)
        } else {
            getString(R.string.login_captcha)
        }
        binding.confirmBtn.isEnabled = !state.loading &&
            state.phone.trim().isNotBlank() &&
            state.code.trim().isNotBlank()
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
