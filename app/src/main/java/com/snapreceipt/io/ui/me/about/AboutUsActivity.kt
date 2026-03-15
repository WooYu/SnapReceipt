package com.snapreceipt.io.ui.me.about

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.snapreceipt.io.config.settings.SettingsManager
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.snapreceipt.io.BuildConfig
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityAboutUsBinding
import com.snapreceipt.io.domain.model.PolicyEntity
import com.snapreceipt.io.domain.usecase.config.FetchPolicyUseCase
import com.snapreceipt.io.ui.common.PolicyWebViewActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AboutUsActivity : BaseActivity<BaseViewModel>() {

    @Inject
    lateinit var fetchPolicyUseCase: FetchPolicyUseCase

    @Inject
    lateinit var settingsManager: SettingsManager
    private var _binding: ActivityAboutUsBinding? = null
    private val binding get() = _binding!!
    private var policyCache: PolicyEntity? = null
    private var policyPrefetchJob: kotlinx.coroutines.Job? = null
    private var testingOptionsTapCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityAboutUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appVersion.text = "V${BuildConfig.VERSION_NAME}"

        prefetchPolicy()
        binding.pageHeader.setOnLeftIconClickListener { finish() }
        binding.appVersion.setOnClickListener { unlockTestingOptionsIfNeeded() }
        binding.menuUserAgreement.setOnClickListener {
            openPolicyUrl(isUserAgreement = true)
        }
        binding.menuPrivacyPolicy.setOnClickListener {
            openPolicyUrl(isUserAgreement = false)
        }
    }

    override fun onDestroy() {
        policyPrefetchJob?.cancel()
        policyPrefetchJob = null
        _binding?.pageHeader?.setOnLeftIconClickListener(null)
        _binding?.appVersion?.setOnClickListener(null)
        _binding?.menuUserAgreement?.setOnClickListener(null)
        _binding?.menuPrivacyPolicy?.setOnClickListener(null)
        _binding = null
        super.onDestroy()
    }

    private fun openPolicyUrl(isUserAgreement: Boolean) {
        lifecycleScope.launch {
            val prefetch = policyPrefetchJob
            if (policyCache == null && prefetch?.isActive == true) {
                prefetch.join()
            }
            val policy = policyCache
            if (policy == null) {
                Toast.makeText(
                    this@AboutUsActivity,
                    getString(R.string.unexpected_error),
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            val url = if (isUserAgreement) {
                policy.userAgreement
            } else {
                policy.privacyPolicy
            }
            val title = if (isUserAgreement) {
                getString(R.string.user_agreement)
            } else {
                getString(R.string.privacy_policy_label)
            }
            openUrl(url, title)
        }
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

    private fun prefetchPolicy() {
        if (policyCache != null || policyPrefetchJob?.isActive == true) return
        policyPrefetchJob = lifecycleScope.launch {
            fetchPolicyUseCase()
                .onSuccess { policyCache = it }
        }
    }

    private fun unlockTestingOptionsIfNeeded() {
        testingOptionsTapCount += 1
        val remainingTaps = TESTING_OPTIONS_UNLOCK_TAP_COUNT - testingOptionsTapCount
        if (remainingTaps > 0) {
            if (remainingTaps <= TESTING_OPTIONS_HINT_THRESHOLD) {
                Toast.makeText(
                    this,
                    getString(R.string.testing_options_unlock_countdown, remainingTaps),
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        testingOptionsTapCount = 0
        lifecycleScope.launch {
            settingsManager.setInternalTestingOptionsEnabled(true)
            Toast.makeText(
                this@AboutUsActivity,
                getString(R.string.testing_options_unlocked),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        private const val TESTING_OPTIONS_UNLOCK_TAP_COUNT = 7
        private const val TESTING_OPTIONS_HINT_THRESHOLD = 3
    }
}
