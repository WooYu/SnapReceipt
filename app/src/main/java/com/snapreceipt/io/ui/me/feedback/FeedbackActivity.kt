package com.snapreceipt.io.ui.me.feedback

import android.os.Bundle
import com.snapreceipt.io.R
import com.snapreceipt.io.ui.common.EdgeToEdgeActivity

class FeedbackActivity : EdgeToEdgeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        findViewById<android.view.View>(R.id.btn_back).setOnClickListener { finish() }
    }
}
