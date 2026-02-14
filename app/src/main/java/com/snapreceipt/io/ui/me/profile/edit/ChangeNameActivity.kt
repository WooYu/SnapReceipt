package com.snapreceipt.io.ui.me.profile.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.observeState
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityChangeNameBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangeNameActivity : BaseActivity<ChangeNameViewModel>() {

    override val viewModel: ChangeNameViewModel by viewModels()

    private var _binding: ActivityChangeNameBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityChangeNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pageHeader.setOnLeftIconClickListener { finish() }
        binding.confirmBtn.setOnClickListener { viewModel.submit() }
        binding.nameInput.doAfterTextChanged { viewModel.updateName(it?.toString().orEmpty()) }

        val initialName = intent.getStringExtra(EXTRA_NAME).orEmpty()
        binding.nameInput.setText(initialName)
        binding.nameInput.setSelection(initialName.length)
        viewModel.updateName(initialName)

        observeState(viewModel.uiState) { renderState(it) }
    }

    override fun onDestroy() {
        _binding?.pageHeader?.setOnLeftIconClickListener(null)
        _binding?.confirmBtn?.setOnClickListener(null)
        _binding = null
        super.onDestroy()
    }

    private fun renderState(state: ChangeNameUiState) {
        if (state.loading) {
            showGlobalLoading(getString(R.string.loading_please_wait_dynamic))
        } else {
            hideGlobalLoading()
        }
        binding.confirmBtn.isEnabled = !state.loading && state.name.trim().isNotBlank()
    }

    companion object {
        private const val EXTRA_NAME = "extra_name"

        fun createIntent(context: Context, currentName: String): Intent {
            return Intent(context, ChangeNameActivity::class.java).apply {
                putExtra(EXTRA_NAME, currentName)
            }
        }
    }
}
