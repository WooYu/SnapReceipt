package com.snapreceipt.io

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.snapreceipt.io.databinding.ActivityMainBinding
import com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity
import com.snapreceipt.io.ui.main.MainViewModel
import com.snapreceipt.io.ui.login.LoginActivity
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.core.network.auth.SessionEvent
import com.skybound.space.core.network.auth.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<MainViewModel>() {
    override val viewModel: MainViewModel by viewModels()
    @Inject
    lateinit var injectedSessionManager: SessionManager
    override val sessionManager: SessionManager
        get() = injectedSessionManager

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        // 强制清除图标 tint，使自定义 selector 图标显示原始颜色（主题可能覆盖 XML 的 itemIconTint="@null"）
        binding.bottomNav.itemIconTintList = null

        if (savedInstanceState == null) {
            val startTab = intent.getStringExtra(InvoiceDetailsActivity.EXTRA_START_TAB)
            if (startTab == InvoiceDetailsActivity.TAB_RECEIPTS) {
                navController.navigate(
                    R.id.nav_receipts,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_home, true, true)
                        .build()
                )
            }
        }
    }

    fun setBottomNavVisible(visible: Boolean) {
        binding.bottomNav.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onSessionExpired(event: SessionEvent) {
        startActivity(
            android.content.Intent(this, LoginActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        finish()
    }
}
