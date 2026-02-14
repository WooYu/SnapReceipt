package com.snapreceipt.io.ui.me.feedback

import android.os.Bundle
import com.snapreceipt.io.databinding.ActivityFeedbackBinding
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.viewmodel.BaseViewModel

class FeedbackActivity : BaseActivity<BaseViewModel>() {
    private var _binding: ActivityFeedbackBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pageHeader.setOnLeftIconClickListener { finish() }
    }

    override fun onDestroy() {
        _binding?.pageHeader?.setOnLeftIconClickListener(null)
        _binding = null
        super.onDestroy()
    }
}
