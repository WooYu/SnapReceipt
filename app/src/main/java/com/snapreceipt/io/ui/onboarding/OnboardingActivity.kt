package com.snapreceipt.io.ui.onboarding

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.config.settings.SettingsManager
import com.snapreceipt.io.databinding.ActivityOnboardingBinding
import com.snapreceipt.io.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    private lateinit var binding: ActivityOnboardingBinding
    private var isCompleting = false
    private var baseSkipTopMargin = 0
    private var baseStartBottomMargin = 0

    private val onboardingPages = listOf(
        R.drawable.img_onboarding_1,
        R.drawable.img_onboarding_2,
        R.drawable.img_onboarding_3
    )

    private val pageChangeCallback =
        object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateControls(position)
            }
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

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        baseSkipTopMargin =
            (binding.skipButton.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        baseStartBottomMargin =
            (binding.startButton.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        applySystemBarInsets()

        binding.onboardingPager.adapter = OnboardingPagerAdapter(onboardingPages)
        binding.onboardingPager.offscreenPageLimit = onboardingPages.size
        (binding.onboardingPager.getChildAt(0) as? RecyclerView)?.overScrollMode =
            View.OVER_SCROLL_NEVER
        binding.onboardingPager.registerOnPageChangeCallback(pageChangeCallback)

        binding.skipButton.setOnClickListener {
            completeOnboarding()
        }
        binding.startButton.setOnClickListener {
            completeOnboarding()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = binding.onboardingPager.currentItem
                if (current > 0) {
                    binding.onboardingPager.currentItem = current - 1
                } else {
                    finish()
                }
            }
        })

        updateControls(0)
    }

    override fun onDestroy() {
        binding.onboardingPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroy()
    }

    private fun updateControls(position: Int) {
        val isLastPage = position == onboardingPages.lastIndex
        binding.skipButton.isVisible = !isLastPage
        binding.startButtonScrim.isVisible = isLastPage
        binding.startButton.isVisible = isLastPage
    }

    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.skipButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = baseSkipTopMargin + systemInsets.top
            }
            binding.startButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = baseStartBottomMargin + systemInsets.bottom
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun completeOnboarding() {
        if (isCompleting) return
        isCompleting = true
        lifecycleScope.launch {
            runCatching {
                settingsManager.setOnboardingCompleted(true)
            }
            startActivity(Intent(this@OnboardingActivity, LoginActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
