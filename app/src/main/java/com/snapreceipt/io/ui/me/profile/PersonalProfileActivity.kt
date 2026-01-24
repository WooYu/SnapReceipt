package com.snapreceipt.io.ui.me.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.snapreceipt.io.R

class PersonalProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_profile)

        findViewById<android.view.View>(R.id.btn_back).setOnClickListener { finish() }
    }
}
