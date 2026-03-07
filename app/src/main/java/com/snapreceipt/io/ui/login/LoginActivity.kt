package com.snapreceipt.io.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import android.widget.Toast
import com.snapreceipt.io.MainActivity
import com.snapreceipt.io.R
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.observeState
import com.skybound.space.core.network.auth.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.snapreceipt.io.ui.common.PolicyWebViewActivity

@AndroidEntryPoint
class LoginActivity : BaseActivity<LoginViewModel>() {
    override val viewModel: LoginViewModel by viewModels()
    @Inject
    lateinit var injectedSessionManager: SessionManager
    override val sessionManager: SessionManager
        get() = injectedSessionManager
    private var currentMode: LoginMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (sessionManager.hasActiveSession()) {
            navigateToMainActivity()
            return
        }
        setContentView(R.layout.activity_login)
        setupBackPress()

        if (savedInstanceState == null) {
            viewModel.switchToEmail()
        }
        observeState(viewModel.uiState) { renderState(it) }
    }

    private fun renderState(state: LoginUiState) {
        updateLoginLoading(state)
        if (currentMode == state.mode) return
        currentMode = state.mode
        val fragment = when (state.mode) {
            LoginMode.PHONE -> PhoneLoginFragment()
            LoginMode.EMAIL -> EmailLoginFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun updateLoginLoading(state: LoginUiState) {
        val message = when {
            state.requestingCode -> getString(R.string.login_requesting_code)
            state.loading -> getString(R.string.loading_please_wait_dynamic)
            else -> null
        }
        if (message.isNullOrBlank()) {
            hideGlobalLoading()
        } else {
            showGlobalLoading(message)
        }
    }

    override fun onCustomEvent(event: UiEvent.Custom) {
        when (event.type) {
            LoginEventKeys.CODE_SENT -> {
                val target = event.payload?.getString(LoginEventKeys.EXTRA_TARGET).orEmpty()
                if (target.isNotEmpty()) {
                    Toast.makeText(this, getString(R.string.code_sent, target), Toast.LENGTH_SHORT).show()
                }
            }
            LoginEventKeys.NAVIGATE_MAIN -> navigateToMainActivity()
            LoginEventKeys.OPEN_POLICY -> {
                val url = event.payload?.getString(LoginEventKeys.EXTRA_URL).orEmpty()
                val type = event.payload?.getString(LoginEventKeys.EXTRA_POLICY_TYPE)
                val title = when (type) {
                    "USER_AGREEMENT" -> getString(R.string.user_agreement)
                    "PRIVACY_POLICY" -> getString(R.string.privacy_policy_label)
                    else -> getString(R.string.agreement_dialog_title)
                }
                openUrl(url, title)
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun openUrl(url: String, title: String? = null) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            Toast.makeText(this, getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, PolicyWebViewActivity::class.java).apply {
                putExtra(PolicyWebViewActivity.EXTRA_URL, trimmed)
                if (!title.isNullOrBlank()) {
                    putExtra(PolicyWebViewActivity.EXTRA_TITLE, title)
                }
            }
        )
    }

}
