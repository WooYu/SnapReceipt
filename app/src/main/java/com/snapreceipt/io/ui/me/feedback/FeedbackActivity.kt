package com.snapreceipt.io.ui.me.feedback

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.skybound.space.base.presentation.BaseActivity
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityFeedbackBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.max

@AndroidEntryPoint
class FeedbackActivity : BaseActivity<FeedbackViewModel>() {

    companion object {
        private const val MAX_INPUT_LENGTH = 1000
    }

    override val viewModel: FeedbackViewModel by viewModels()

    private var _binding: ActivityFeedbackBinding? = null
    private val binding get() = _binding!!

    private var loadingDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        _binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets()

        initView()
        observeViewModel()
    }

    private fun initView() {
        binding.pageHeader.setOnLeftIconClickListener { finish() }

        binding.feedbackCharCount.text = getString(R.string.char_count_format, 0, MAX_INPUT_LENGTH)

        binding.feedbackInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                binding.feedbackCharCount.text = getString(R.string.char_count_format, length, MAX_INPUT_LENGTH)
                binding.feedbackInput.post {
                    binding.feedbackInput.bringPointIntoView(binding.feedbackInput.selectionEnd)
                }
                binding.submitBtn.isEnabled = length > 0
            }
        })

        binding.submitBtn.setOnClickListener {
            val content = binding.feedbackInput.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.submit(content)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loading.collect { isLoading ->
                    showLoading(isLoading)
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        if (!show) {
            dismissLoading()
            return
        }
        if (isFinishing || isDestroyed) return
        val dialog = loadingDialog ?: createLoadingDialog().also { loadingDialog = it }
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    private fun dismissLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun createLoadingDialog(): Dialog {
        return Dialog(this).apply {
            setContentView(com.skybound.space.base.R.layout.dialog_fragment_loading)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissLoading()
        _binding = null
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(bottom = max(systemBars.bottom, ime.bottom))
            insets
        }
    }
}
