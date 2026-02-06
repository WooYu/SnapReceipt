package com.snapreceipt.io.ui.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
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
        bindMenuItems()
        setupListeners()
        observeState(viewModel.uiState) { renderState(it) }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        Glide.with(this).clear(binding.ivAvatar)
        super.onDestroyView()
        _binding = null
    }

    private fun renderState(state: MeUiState) {
        binding.username.text = state.username.ifBlank { getString(R.string.placeholder_dash) }
        binding.email.text = state.email.ifBlank { getString(R.string.placeholder_dash) }
        renderAvatar(state.avatar)
    }

    override fun onCustomEvent(event: UiEvent.Custom) {
        if (event.type == MeEventKeys.NAVIGATE_LOGIN) {
            navigateToLogin()
        }
    }

    private fun bindMenuItems() {
        bindMenuItem(R.id.menu_export, R.drawable.ic_me_export, R.string.export_records)
        bindMenuItem(R.id.menu_about, R.drawable.ic_me_about, R.string.about_us)
        bindMenuItem(R.id.menu_feedback, R.drawable.ic_me_feedback, R.string.feedback)
        bindMenuItem(R.id.menu_settings, R.drawable.ic_me_settings, R.string.settings)
    }

    private fun bindMenuItem(
        @IdRes menuId: Int,
        @DrawableRes iconRes: Int,
        @StringRes titleRes: Int
    ) {
        val row = menuView(menuId)
        row.findViewById<ImageView>(R.id.menu_icon).setImageResource(iconRes)
        row.findViewById<TextView>(R.id.menu_title).setText(titleRes)
        row.contentDescription = getString(titleRes)
    }

    private fun setupListeners() {
        binding.editProfileBtn.setOnClickListener {
            startActivity(Intent(requireContext(), PersonalProfileActivity::class.java))
        }
        menuView(R.id.menu_export).setOnClickListener {
            startActivity(Intent(requireContext(), ExportRecordsActivity::class.java))
        }
        menuView(R.id.menu_settings).setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        menuView(R.id.menu_feedback).setOnClickListener {
            startActivity(Intent(requireContext(), FeedbackActivity::class.java))
        }
        menuView(R.id.menu_about).setOnClickListener {
            startActivity(Intent(requireContext(), AboutUsActivity::class.java))
        }
    }

    private fun menuView(@IdRes id: Int): View = binding.root.findViewById(id)

    private fun navigateToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        activity?.finish()
    }

    private fun renderAvatar(avatarUrl: String) {
        val fallback = R.drawable.img_app_icon
        if (avatarUrl.isBlank()) {
            binding.ivAvatar.setImageResource(fallback)
            return
        }
        Glide.with(this)
            .load(avatarUrl)
            .placeholder(fallback)
            .error(fallback)
            .centerCrop()
            .into(binding.ivAvatar)
    }
}
