package com.snapreceipt.io.ui.me.settings

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.snapreceipt.io.R
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
    private lateinit var cacheSizeView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        cacheSizeView = findViewById(R.id.cache_size)
        findViewById<android.view.View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.menu_switch_account).setOnClickListener {
            Toast.makeText(this, getString(R.string.switch_account), Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.menu_clear_cache).setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { clearAppCache() }
                Toast.makeText(this@SettingsActivity, getString(R.string.clear_cache), Toast.LENGTH_SHORT).show()
                updateCacheSize()
            }
        }
        findViewById<android.view.View>(R.id.logout_btn).setOnClickListener {
            sessionManager.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        updateCacheSize()
    }

    private fun updateCacheSize() {
        lifecycleScope.launch {
            val sizeBytes = withContext(Dispatchers.IO) {
                directorySize(cacheDir) + (externalCacheDir?.let { directorySize(it) } ?: 0L)
            }
            cacheSizeView.text = formatSize(sizeBytes)
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
