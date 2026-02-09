package com.snapreceipt.io.ui.me.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.network.auth.SessionManager
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivitySettingsBinding
import com.snapreceipt.io.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : BaseActivity<BaseViewModel>() {

    @Inject
    lateinit var injectedSessionManager: SessionManager
    override val sessionManager: SessionManager
        get() = injectedSessionManager
    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()
        binding.pageHeader.setOnLeftIconClickListener { finish() }

        binding.menuClearCache.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { clearAppCache() }
                Toast.makeText(
                    this@SettingsActivity,
                    getString(R.string.clear_cache),
                    Toast.LENGTH_SHORT
                ).show()
                updateCacheSize()
            }
        }
        binding.logoutBtn.setOnClickListener {
            sessionManager.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        updateCacheSize()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun updateCacheSize() {
        lifecycleScope.launch {
            val sizeBytes = withContext(Dispatchers.IO) {
                directorySize(cacheDir) + (externalCacheDir?.let { directorySize(it) } ?: 0L)
            }
            binding.menuClearCache.setValueText(formatSize(sizeBytes))
        }
    }

    private fun clearAppCache() {
        deleteContents(cacheDir)
        externalCacheDir?.let { deleteContents(it) }
    }

    private fun deleteContents(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        }
    }

    private fun directorySize(dir: File): Long {
        var total = 0L
        dir.listFiles()?.forEach { file ->
            total += if (file.isDirectory) {
                directorySize(file)
            } else {
                file.length()
            }
        }
        return total
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var idx = 0
        while (value >= 1024 && idx < units.lastIndex) {
            value /= 1024
            idx++
        }
        val formatted = if (idx == 0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
        return formatted + units[idx]
    }

    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }
}
