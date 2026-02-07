package com.snapreceipt.io.ui.me.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivitySettingsBinding
import com.snapreceipt.io.ui.common.EdgeToEdgeActivity
import com.snapreceipt.io.ui.login.LoginActivity
import com.skybound.space.core.network.auth.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : EdgeToEdgeActivity() {

    @Inject
    lateinit var sessionManager: SessionManager
    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pageHeader.title.setText(R.string.settings)
        binding.menuSwitchAccount.menuTitle.setText(R.string.switch_account)
        binding.menuClearCache.menuTitle.setText(R.string.clear_cache)
        binding.menuClearCache.menuValue.visibility = View.VISIBLE
        binding.pageHeader.btnBack.setOnClickListener { finish() }
        binding.menuSwitchAccount.root.setOnClickListener {
            Toast.makeText(this, getString(R.string.switch_account), Toast.LENGTH_SHORT).show()
        }
        binding.menuClearCache.root.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { clearAppCache() }
                Toast.makeText(this@SettingsActivity, getString(R.string.clear_cache), Toast.LENGTH_SHORT).show()
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
            binding.menuClearCache.menuValue.text = formatSize(sizeBytes)
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
}
