package com.snapreceipt.io.ui.me.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.usecase.config.FetchPolicyUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AboutUsActivity : AppCompatActivity() {

    @Inject
    lateinit var fetchPolicyUseCase: FetchPolicyUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

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
            fetchPolicyUseCase()
                .onSuccess { policy ->
                    val url = if (isUserAgreement) {
                        policy.userAgreement
                    } else {
                        policy.privacyPolicy
                    }
                    openUrl(url)
                }
                .onFailure {
                    Toast.makeText(this@AboutUsActivity, getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
                }
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
}
