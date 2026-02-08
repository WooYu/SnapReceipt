package com.snapreceipt.io.ui.me.feedback

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.snapreceipt.io.databinding.ActivityFeedbackBinding
import com.snapreceipt.io.ui.common.EdgeToEdgeActivity

class FeedbackActivity : EdgeToEdgeActivity() {

    private var _binding: ActivityFeedbackBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pageHeader.setOnLeftIconClickListener { finish() }

        binding.feedbackInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                binding.feedbackCharCount.text = "$length/1000"
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
