package com.snapreceipt.io.ui.splash

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.snapreceipt.io.R
import com.snapreceipt.io.ui.login.LoginActivity

class BrandSplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DELAY_MS = 1400L
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val navigateTask = Runnable {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

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
        mainHandler.postDelayed(navigateTask, SPLASH_DELAY_MS)
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(navigateTask)
        super.onDestroy()
    }
}
