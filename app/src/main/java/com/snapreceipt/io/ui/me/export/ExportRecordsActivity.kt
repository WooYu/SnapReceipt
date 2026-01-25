package com.snapreceipt.io.ui.me.export

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.ui.common.shouldShowEmpty
import com.snapreceipt.io.ui.common.shouldShowNoMore
import com.skybound.space.core.config.AppConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExportRecordsActivity : AppCompatActivity() {

    private val viewModel: ExportRecordsViewModel by viewModels()
    private lateinit var recordsList: RecyclerView
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var emptyState: View
    private lateinit var loadingIndicator: View
    private lateinit var loadMoreIndicator: View
    private lateinit var noMoreHint: View
    private lateinit var adapter: ExportRecordsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_records)

        recordsList = findViewById(R.id.records_list)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        emptyState = findViewById(R.id.empty_state)
        loadingIndicator = findViewById(R.id.loading_indicator)
        loadMoreIndicator = findViewById(R.id.load_more_indicator)
        noMoreHint = findViewById(R.id.no_more_hint)

        adapter = ExportRecordsAdapter { record -> openExportFile(record) }
        val layoutManager = LinearLayoutManager(this)
        recordsList.layoutManager = layoutManager
        recordsList.adapter = adapter
        recordsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val total = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (total > 0 && lastVisible >= total - 3) {
                    viewModel.loadMore()
                }
            }
        })

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

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
        loadingIndicator.visibility = if (state.loading && !state.hasLoaded) View.VISIBLE else View.GONE
        swipeRefresh.isRefreshing = state.refreshing
        loadMoreIndicator.visibility = if (state.loadingMore) View.VISIBLE else View.GONE
        noMoreHint.visibility = if (
            shouldShowNoMore(state.hasLoaded, state.hasMore, state.records.size, state.loadingMore)
        ) View.VISIBLE else View.GONE
        val showEmpty = shouldShowEmpty(state.hasLoaded, state.empty)
        emptyState.visibility = if (showEmpty) View.VISIBLE else View.GONE
        recordsList.visibility = if (showEmpty) View.GONE else View.VISIBLE
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
        val downloadsDir = runCatching {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }.getOrNull()
        val candidates = listOfNotNull(
            java.io.File(filesDir, raw),
            java.io.File(cacheDir, raw),
            externalCacheDir?.let { java.io.File(it, raw) },
            getExternalFilesDir(null)?.let { java.io.File(it, raw) },
            downloadsDir?.let { java.io.File(it, raw) }
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
