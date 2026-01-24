package com.snapreceipt.io.ui.me.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.snapreceipt.io.R
import com.snapreceipt.io.ui.login.LoginActivity
import com.skybound.space.core.network.auth.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<android.view.View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.menu_switch_account).setOnClickListener {
            Toast.makeText(this, getString(R.string.switch_account), Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.menu_clear_cache).setOnClickListener {
            Toast.makeText(this, getString(R.string.clear_cache), Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.logout_btn).setOnClickListener {
            sessionManager.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }
}
