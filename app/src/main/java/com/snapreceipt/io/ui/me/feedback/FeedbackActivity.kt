package com.snapreceipt.io.ui.me.feedback

import android.os.Bundle
import com.snapreceipt.io.databinding.ActivityFeedbackBinding
import com.snapreceipt.io.ui.common.EdgeToEdgeActivity

class FeedbackActivity : EdgeToEdgeActivity() {

    private var _binding: ActivityFeedbackBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
