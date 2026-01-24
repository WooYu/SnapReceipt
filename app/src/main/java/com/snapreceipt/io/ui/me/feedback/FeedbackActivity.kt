package com.snapreceipt.io.ui.me.feedback

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.snapreceipt.io.R

class FeedbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        findViewById<android.view.View>(R.id.btn_back).setOnClickListener { finish() }
    }
}
