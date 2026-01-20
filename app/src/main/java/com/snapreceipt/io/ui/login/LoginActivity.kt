package com.snapreceipt.io.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.snapreceipt.io.MainActivity
import com.snapreceipt.io.R
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.core.network.auth.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            viewModel.switchToPhone()
        }
        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
    }

    private fun renderState(state: LoginUiState) {
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

    override fun onCustomEvent(event: UiEvent.Custom) {
        when (event.type) {
            LoginEventKeys.CODE_SENT -> {
                val target = event.payload?.getString(LoginEventKeys.EXTRA_TARGET).orEmpty()
                if (target.isNotEmpty()) {
                    Toast.makeText(this, getString(R.string.code_sent, target), Toast.LENGTH_SHORT).show()
                }
            }
            LoginEventKeys.NAVIGATE_MAIN -> navigateToMainActivity()
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.uiState.value.mode == LoginMode.EMAIL) {
                    viewModel.switchToPhone()
                } else {
                    finish()
                }
            }
        })
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
