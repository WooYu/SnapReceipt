package com.snapreceipt.io.ui.splash

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.snapreceipt.io.config.settings.SettingsManager
import com.snapreceipt.io.R
import com.snapreceipt.io.ui.login.LoginActivity
import com.snapreceipt.io.ui.onboarding.OnboardingActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BrandSplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DELAY_MS = 1400L
    }

    @Inject
    lateinit var settingsManager: SettingsManager

    private var navigateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContentView(R.layout.activity_brand_splash)
        navigateJob = lifecycleScope.launch {
            delay(SPLASH_DELAY_MS)
            val onboardingCompleted = runCatching {
                settingsManager.isOnboardingCompleted()
            }.getOrDefault(false)
            val target = if (onboardingCompleted) {
                LoginActivity::class.java
            } else {
                OnboardingActivity::class.java
            }
            startActivity(Intent(this@BrandSplashActivity, target))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onDestroy() {
        navigateJob?.cancel()
        super.onDestroy()
    }
}
