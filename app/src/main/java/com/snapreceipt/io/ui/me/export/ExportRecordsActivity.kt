package com.snapreceipt.io.ui.me.export

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.skybound.space.core.config.AppConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExportRecordsActivity : AppCompatActivity() {

    private val viewModel: ExportRecordsViewModel by viewModels()
    private lateinit var recordsList: RecyclerView
    private lateinit var emptyState: View
    private lateinit var loadingIndicator: View
    private lateinit var adapter: ExportRecordsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_records)

        recordsList = findViewById(R.id.records_list)
        emptyState = findViewById(R.id.empty_state)
        loadingIndicator = findViewById(R.id.loading_indicator)

        adapter = ExportRecordsAdapter { record -> openExportFile(record) }
        recordsList.adapter = adapter

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
    }

    private fun renderState(state: ExportRecordsUiState) {
        loadingIndicator.visibility = if (state.loading) View.VISIBLE else View.GONE
        emptyState.visibility = if (state.empty && !state.loading) View.VISIBLE else View.GONE
        recordsList.visibility = if (state.empty) View.GONE else View.VISIBLE
        adapter.submitList(state.records)
    }

    private fun openExportFile(record: ExportRecordEntity) {
        val raw = record.fileUrl.trim()
        if (raw.isBlank()) {
            Toast.makeText(this, getString(R.string.export_file_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        resolveLocalFile(raw)?.let { file ->
            openLocalFile(file)
            return
        }
        val url = resolveRemoteUrl(raw) ?: run {
            Toast.makeText(this, getString(R.string.export_file_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        openRemoteUrl(url)
    }

    private fun resolveRemoteUrl(raw: String): String? {
        if (raw.startsWith("http", ignoreCase = true)) return raw
        val base = AppConfig.baseUrl.trimEnd('/')
        val path = if (raw.startsWith("/")) raw else "/$raw"
        return "$base$path"
    }

    private fun resolveLocalFile(raw: String): java.io.File? {
        val direct = java.io.File(raw)
        if (direct.isAbsolute && direct.exists()) return direct
        val candidates = listOfNotNull(
            java.io.File(filesDir, raw),
            java.io.File(cacheDir, raw),
            externalCacheDir?.let { java.io.File(it, raw) },
            getExternalFilesDir(null)?.let { java.io.File(it, raw) }
        )
        return candidates.firstOrNull { it.exists() }
    }

    private fun openLocalFile(file: java.io.File) {
        Toast.makeText(this, getString(R.string.opening_export_file), Toast.LENGTH_SHORT).show()
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val mime = when {
            file.name.endsWith(".xlsx", true) ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            file.name.endsWith(".xls", true) -> "application/vnd.ms-excel"
            else -> "application/octet-stream"
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.no_app_to_open), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openRemoteUrl(url: String) {
        Toast.makeText(this, getString(R.string.opening_export_file), Toast.LENGTH_SHORT).show()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.no_app_to_open), Toast.LENGTH_SHORT).show()
        }
    }
}
