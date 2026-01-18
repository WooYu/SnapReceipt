package com.snapreceipt.io

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.snapreceipt.io.ui.home.HomeFragment
import com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity
import com.snapreceipt.io.ui.main.MainTab
import com.snapreceipt.io.ui.main.MainUiState
import com.snapreceipt.io.ui.main.MainViewModel
import com.snapreceipt.io.ui.me.MeFragment
import com.snapreceipt.io.ui.receipts.ReceiptsFragment
import com.skybound.space.base.presentation.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity<MainViewModel>() {
    override val viewModel: MainViewModel by viewModels()

    private lateinit var bottomNav: BottomNavigationView
    private var suppressSelection = false
    private var currentTab: MainTab? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            if (suppressSelection) return@setOnItemSelectedListener true
            val tab = when (item.itemId) {
                R.id.nav_home -> MainTab.HOME
                R.id.nav_receipts -> MainTab.RECEIPTS
                R.id.nav_me -> MainTab.ME
                else -> MainTab.HOME
            }
            viewModel.selectTab(tab)
            true
        }

        if (savedInstanceState == null) {
            val startTab = intent.getStringExtra(InvoiceDetailsActivity.EXTRA_START_TAB)
            val initialTab = if (startTab == InvoiceDetailsActivity.TAB_RECEIPTS) {
                MainTab.RECEIPTS
            } else {
                MainTab.HOME
            }
            viewModel.selectTab(initialTab)
        }

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
    }

    private fun renderState(state: MainUiState) {
        if (currentTab == state.selectedTab) return
        currentTab = state.selectedTab

        suppressSelection = true
        bottomNav.selectedItemId = when (state.selectedTab) {
            MainTab.HOME -> R.id.nav_home
            MainTab.RECEIPTS -> R.id.nav_receipts
            MainTab.ME -> R.id.nav_me
        }
        suppressSelection = false

        loadFragment(
            when (state.selectedTab) {
                MainTab.HOME -> HomeFragment()
                MainTab.RECEIPTS -> ReceiptsFragment()
                MainTab.ME -> MeFragment()
            }
        )
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
