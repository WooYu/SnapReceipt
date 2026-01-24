package com.snapreceipt.io.ui.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.snapreceipt.io.R
import com.snapreceipt.io.ui.login.LoginActivity
import com.snapreceipt.io.ui.me.about.AboutUsActivity
import com.snapreceipt.io.ui.me.export.ExportRecordsActivity
import com.snapreceipt.io.ui.me.feedback.FeedbackActivity
import com.snapreceipt.io.ui.me.profile.PersonalProfileActivity
import com.snapreceipt.io.ui.me.settings.SettingsActivity
import com.skybound.space.base.presentation.BaseFragment
import com.skybound.space.base.presentation.UiEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MeFragment : BaseFragment<MeViewModel>(R.layout.fragment_me) {
    override val viewModel: MeViewModel by viewModels()

    private lateinit var editProfileBtn: View
    private lateinit var usernameText: android.widget.TextView
    private lateinit var emailText: android.widget.TextView
    private lateinit var exportBtn: View
    private lateinit var settingsBtn: View
    private lateinit var feedbackBtn: View
    private lateinit var aboutBtn: View
    private lateinit var logoutBtn: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        editProfileBtn = view.findViewById(R.id.edit_profile_btn)
        usernameText = view.findViewById(R.id.username)
        emailText = view.findViewById(R.id.email)
        exportBtn = view.findViewById(R.id.menu_export)
        settingsBtn = view.findViewById(R.id.menu_settings)
        feedbackBtn = view.findViewById(R.id.menu_feedback)
        aboutBtn = view.findViewById(R.id.menu_about)
        logoutBtn = view.findViewById(R.id.logout_btn)

        setupListeners()
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

    private fun renderState(state: MeUiState) {
        usernameText.text = state.username.ifBlank { getString(R.string.placeholder_dash) }
        emailText.text = state.email.ifBlank { getString(R.string.placeholder_dash) }
    }

    override fun onCustomEvent(event: UiEvent.Custom) {
        if (event.type == MeEventKeys.NAVIGATE_LOGIN) {
            navigateToLogin()
        }
    }

    private fun setupListeners() {
        editProfileBtn.setOnClickListener {
            startActivity(Intent(requireContext(), PersonalProfileActivity::class.java))
        }
        exportBtn.setOnClickListener {
            startActivity(Intent(requireContext(), ExportRecordsActivity::class.java))
        }
        settingsBtn.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        feedbackBtn.setOnClickListener {
            startActivity(Intent(requireContext(), FeedbackActivity::class.java))
        }
        aboutBtn.setOnClickListener {
            startActivity(Intent(requireContext(), AboutUsActivity::class.java))
        }
        logoutBtn.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        activity?.finish()
    }
}
