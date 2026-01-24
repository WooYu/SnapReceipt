package com.snapreceipt.io.ui.me.about

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.snapreceipt.io.R

class AboutUsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        findViewById<android.view.View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.menu_user_agreement).setOnClickListener {
            Toast.makeText(this, getString(R.string.user_agreement), Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.menu_privacy_policy).setOnClickListener {
            Toast.makeText(this, getString(R.string.privacy_policy_label), Toast.LENGTH_SHORT).show()
        }
    }
}
