package com.snapreceipt.io

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.snapreceipt.io.ui.home.HomeFragment
import com.snapreceipt.io.ui.receipts.ReceiptsFragment
import com.snapreceipt.io.ui.me.MeFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_receipts -> {
                    loadFragment(ReceiptsFragment())
                    true
                }
                R.id.nav_me -> {
                    loadFragment(MeFragment())
                    true
                }
                else -> false
            }
        }

        // Load home fragment by default
        if (savedInstanceState == null) {
            val startTab = intent.getStringExtra(
                com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.EXTRA_START_TAB
            )
            bottomNav.selectedItemId = if (startTab ==
                com.snapreceipt.io.ui.invoice.InvoiceDetailsActivity.TAB_RECEIPTS
            ) {
                R.id.nav_receipts
            } else {
                R.id.nav_home
            }
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
