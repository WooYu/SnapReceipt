package com.snapreceipt.io.ui.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.FragmentMeBinding
import com.snapreceipt.io.ui.login.LoginActivity
import com.snapreceipt.io.ui.me.about.AboutUsActivity
import com.snapreceipt.io.ui.me.export.ExportRecordsActivity
import com.snapreceipt.io.ui.me.feedback.FeedbackActivity
import com.snapreceipt.io.ui.me.profile.PersonalProfileActivity
import com.snapreceipt.io.ui.me.settings.SettingsActivity
import com.skybound.space.base.presentation.BaseFragment
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.observeState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MeFragment : BaseFragment<MeViewModel>(R.layout.fragment_me) {
    override val viewModel: MeViewModel by viewModels()

    private var _binding: FragmentMeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMeBinding.bind(view)
        setupListeners()
        observeState()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderState(state: MeUiState) {
        binding.username.text = state.username.ifBlank { getString(R.string.placeholder_dash) }
        binding.email.text = state.email.ifBlank { getString(R.string.placeholder_dash) }
    }

    override fun onCustomEvent(event: UiEvent.Custom) {
        if (event.type == MeEventKeys.NAVIGATE_LOGIN) {
            navigateToLogin()
        }
    }

    private fun setupListeners() {
        binding.editProfileBtn.setOnClickListener {
            startActivity(Intent(requireContext(), PersonalProfileActivity::class.java))
        }
        binding.menuExport.setOnClickListener {
            startActivity(Intent(requireContext(), ExportRecordsActivity::class.java))
        }
        binding.menuSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        binding.menuFeedback.setOnClickListener {
            startActivity(Intent(requireContext(), FeedbackActivity::class.java))
        }
        binding.menuAbout.setOnClickListener {
            startActivity(Intent(requireContext(), AboutUsActivity::class.java))
        }
        binding.logoutBtn.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        activity?.finish()
    }
}
