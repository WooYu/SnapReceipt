package com.snapreceipt.io.ui.me.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.snapreceipt.io.config.settings.AppSettings
import com.snapreceipt.io.config.settings.SettingsManager
import com.snapreceipt.io.monitoring.diagnostic.DiagnosticLogManager
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.network.auth.SessionManager
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivitySettingsBinding
import com.snapreceipt.io.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : BaseActivity<BaseViewModel>() {

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var diagnosticLogManager: DiagnosticLogManager

    @Inject
    lateinit var injectedSessionManager: SessionManager
    override val sessionManager: SessionManager
        get() = injectedSessionManager
    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding!!
    private var currentSettings: AppSettings = AppSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()
        binding.pageHeader.setOnLeftIconClickListener { finish() }
        bindActions()
        observeSettings()
        updateCacheSize()
    }

    private fun bindActions() {
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
        binding.menuDiagnosticLogging.setOnClickListener {
            updateToggleSetting(
                enabled = !currentSettings.enableDiagnosticFileLogging,
                label = getString(R.string.diagnostic_logging_mode)
            ) { settingsManager.setDiagnosticFileLogging(it) }
        }
        binding.menuCrashReporting.setOnClickListener {
            updateToggleSetting(
                enabled = !currentSettings.enableCrashReporting,
                label = getString(R.string.crash_reporting)
            ) { settingsManager.setCrashReporting(it) }
        }
        binding.menuAnalyticsReporting.setOnClickListener {
            updateToggleSetting(
                enabled = !currentSettings.enableAnalytics,
                label = getString(R.string.analytics_reporting)
            ) { settingsManager.setAnalytics(it) }
        }
        binding.menuExportDiagnostics.setOnClickListener {
            exportDiagnosticLogs()
        }
        binding.logoutBtn.setOnClickListener {
            sessionManager.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsManager.settings.collectLatest { settings ->
                    currentSettings = settings
                    renderSettings(settings)
                }
            }
        }
    }

    private fun renderSettings(settings: AppSettings) {
        renderInternalTestingOptions(settings.showInternalTestingOptions)
        binding.menuDiagnosticLogging.setValueText(toggleStateText(settings.enableDiagnosticFileLogging))
        binding.menuCrashReporting.setValueText(toggleStateText(settings.enableCrashReporting))
        binding.menuAnalyticsReporting.setValueText(toggleStateText(settings.enableAnalytics))
    }

    private fun renderInternalTestingOptions(visible: Boolean) {
        binding.menuDiagnosticLogging.isVisible = visible
        binding.menuExportDiagnostics.isVisible = visible
        binding.menuCrashReporting.isVisible = visible
        binding.menuAnalyticsReporting.isVisible = visible
    }

    override fun onDestroy() {
        _binding?.pageHeader?.setOnLeftIconClickListener(null)
        _binding?.menuClearCache?.setOnClickListener(null)
        _binding?.menuDiagnosticLogging?.setOnClickListener(null)
        _binding?.menuCrashReporting?.setOnClickListener(null)
        _binding?.menuAnalyticsReporting?.setOnClickListener(null)
        _binding?.menuExportDiagnostics?.setOnClickListener(null)
        _binding?.logoutBtn?.setOnClickListener(null)
        _binding = null
        super.onDestroy()
    }

    private fun updateToggleSetting(
        enabled: Boolean,
        label: String,
        updateAction: suspend (Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                updateAction(enabled)
                Toast.makeText(
                    this@SettingsActivity,
                    getString(
                        if (enabled) R.string.setting_enabled_feedback else R.string.setting_disabled_feedback,
                        label
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Throwable) {
                Toast.makeText(
                    this@SettingsActivity,
                    getString(R.string.unexpected_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun exportDiagnosticLogs() {
        lifecycleScope.launch {
            diagnosticLogManager.createShareIntent()
                .onSuccess { shareIntent ->
                    try {
                        startActivity(
                            Intent.createChooser(
                                shareIntent,
                                getString(R.string.share_diagnostic_logs)
                            )
                        )
                    } catch (error: ActivityNotFoundException) {
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.no_app_to_open),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .onFailure { error ->
                    val messageRes = if (error is IllegalArgumentException) {
                        R.string.diagnostic_log_export_empty
                    } else {
                        R.string.diagnostic_log_export_failed
                    }
                    Toast.makeText(
                        this@SettingsActivity,
                        getString(messageRes),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
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

    private fun toggleStateText(enabled: Boolean): String {
        return getString(if (enabled) R.string.settings_switch_on else R.string.settings_switch_off)
    }

    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }
}
