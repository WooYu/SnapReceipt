package com.snapreceipt.io.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

    private val onboardingPages = listOf(
        R.drawable.img_onboarding_1,
        R.drawable.img_onboarding_2,
        R.drawable.img_onboarding_3
    )

    private val pageChangeCallback = object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateControls(position)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.onboardingPager.adapter = OnboardingPagerAdapter(onboardingPages)
        binding.onboardingPager.registerOnPageChangeCallback(pageChangeCallback)

        binding.skipButton.setOnClickListener {
            completeOnboarding()
        }

        binding.nextButton.setOnClickListener {
            val current = binding.onboardingPager.currentItem
            if (current < onboardingPages.lastIndex) {
                binding.onboardingPager.currentItem = current + 1
            } else {
                completeOnboarding()
            }
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
        binding.nextButton.text = getString(
            if (isLastPage) R.string.onboarding_get_started else R.string.onboarding_next
        )
        binding.skipButton.visibility = if (isLastPage) View.INVISIBLE else View.VISIBLE
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
