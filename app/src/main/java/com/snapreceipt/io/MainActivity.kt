package com.snapreceipt.io

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.snapreceipt.io.databinding.ActivityMainBinding
import com.snapreceipt.io.ui.common.navigateToLoginOnSessionExpired
import com.snapreceipt.io.ui.invoice.InvoiceDetailsArgsCodec
import com.snapreceipt.io.ui.main.MainViewModel
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.core.network.auth.SessionEvent
import com.skybound.space.core.network.auth.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 应用主容器页。
 *
 * 负责承载底部导航和主导航图，同时统一处理会话失效后的登录跳转。
 */
@AndroidEntryPoint
class MainActivity : BaseActivity<MainViewModel>() {
    override val viewModel: MainViewModel by viewModels()
    override val useDefaultNavigationBarInsets: Boolean = false
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
            val startTab = intent.getStringExtra(InvoiceDetailsArgsCodec.EXTRA_START_TAB)
            if (startTab == InvoiceDetailsArgsCodec.TAB_RECEIPTS) {
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

    /**
     * 控制底部导航显隐，供内部页面在全屏编辑等场景下临时隐藏导航栏。
     */
    fun setBottomNavVisible(visible: Boolean) {
        binding.bottomNav.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * 显示全局 loading 遮罩。
     *
     * @param messageRes 可选提示文案资源；为空时仅显示默认 loading
     */
    fun showGlobalLoadingOverlay(@StringRes messageRes: Int? = null) {
        if (messageRes != null) {
            showGlobalLoading(getString(messageRes))
        } else {
            showGlobalLoading(null)
        }
    }

    /**
     * 隐藏全局 loading 遮罩。
     */
    fun hideGlobalLoadingOverlay() {
        hideGlobalLoading()
    }

    override fun onSessionExpired(event: SessionEvent) = navigateToLoginOnSessionExpired(event)
}
