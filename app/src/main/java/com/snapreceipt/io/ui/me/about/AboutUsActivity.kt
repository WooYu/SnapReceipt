package com.snapreceipt.io.ui.me.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.snapreceipt.io.BuildConfig
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.PolicyEntity
import com.snapreceipt.io.domain.usecase.config.FetchPolicyUseCase
import com.snapreceipt.io.ui.common.EdgeToEdgeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AboutUsActivity : EdgeToEdgeActivity() {

    @Inject
    lateinit var fetchPolicyUseCase: FetchPolicyUseCase
    private var policyCache: PolicyEntity? = null
    private var policyPrefetchJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        findViewById<TextView>(R.id.app_version).text = "V${BuildConfig.VERSION_NAME}"
        prefetchPolicy()
        findViewById<android.view.View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.menu_user_agreement).setOnClickListener {
            openPolicyUrl(isUserAgreement = true)
        }
        findViewById<android.view.View>(R.id.menu_privacy_policy).setOnClickListener {
            openPolicyUrl(isUserAgreement = false)
        }
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
            openUrl(url)
        }
    }

    private fun openUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            Toast.makeText(this, getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trimmed)))
    }

    private fun prefetchPolicy() {
        if (policyCache != null || policyPrefetchJob?.isActive == true) return
        policyPrefetchJob = lifecycleScope.launch {
            fetchPolicyUseCase()
                .onSuccess { policyCache = it }
        }
    }
}
