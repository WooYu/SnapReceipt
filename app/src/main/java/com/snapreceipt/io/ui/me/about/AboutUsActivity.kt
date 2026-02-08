package com.snapreceipt.io.ui.me.about

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.snapreceipt.io.BuildConfig
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityAboutUsBinding
import com.snapreceipt.io.domain.model.PolicyEntity
import com.snapreceipt.io.domain.usecase.config.FetchPolicyUseCase
import com.snapreceipt.io.ui.common.PolicyWebViewActivity
import com.snapreceipt.io.ui.common.EdgeToEdgeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AboutUsActivity : EdgeToEdgeActivity() {

    @Inject
    lateinit var fetchPolicyUseCase: FetchPolicyUseCase
    private var _binding: ActivityAboutUsBinding? = null
    private val binding get() = _binding!!
    private var policyCache: PolicyEntity? = null
    private var policyPrefetchJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityAboutUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appVersion.text = "V${BuildConfig.VERSION_NAME}"

        prefetchPolicy()
        binding.pageHeader.setOnLeftIconClickListener { finish() }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun openPolicyUrl(isUserAgreement: Boolean) {
        lifecycleScope.launch {
            val prefetch = policyPrefetchJob
            if (policyCache == null && prefetch?.isActive == true) {
                prefetch.join()
            }
            val policy = policyCache
            if (policy == null) {
                Toast.makeText(this@AboutUsActivity, getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
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
}
