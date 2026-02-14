package com.snapreceipt.io.ui.me.export

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.observeState
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityExportRecordsBinding
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.ui.widget.statefullist.StatefulListLayout
import com.skybound.space.core.config.AppConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExportRecordsActivity : BaseActivity<ExportRecordsViewModel>() {

    override val viewModel: ExportRecordsViewModel by viewModels()
    private var _binding: ActivityExportRecordsBinding? = null
    private val binding get() = _binding!!
    private var adapter: ExportRecordsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityExportRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recordsAdapter = ExportRecordsAdapter { record -> openExportFile(record) }
        adapter = recordsAdapter
        binding.statefulList.setAdapter(recordsAdapter)
        binding.statefulList.setOnLoadMoreListener { viewModel.loadMore() }
        binding.statefulList.setOnRetryListener { viewModel.loadRecords() }
        
        // RecyclerView in this page needs horizontal padding
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.page_start_margin)
        binding.statefulList.recyclerView.setPadding(
            horizontalPadding, 0, horizontalPadding,
            binding.statefulList.recyclerView.paddingBottom
        )

        binding.pageHeader.setOnLeftIconClickListener { finish() }

        observeState(viewModel.uiState) { renderState(it) }
    }

    override fun onDestroy() {
        _binding?.pageHeader?.setOnLeftIconClickListener(null)
        _binding?.statefulList?.setOnLoadMoreListener(null)
        _binding?.statefulList?.setOnRetryListener(null)
        adapter = null
        _binding = null
        super.onDestroy()
    }

    /**
     * Renders UI state.
     * Data is submitted before state to ensure ListAdapter's async DiffUtil
     * completes before footer state changes (prevents auto-scroll).
     */
    private fun renderState(state: ExportRecordsUiState) {
        val recordsAdapter = adapter ?: return
        // Submit data first (ListAdapter's submitList is async)
        recordsAdapter.submitList(state.records) {
            // After data is committed, update footer state
            _binding?.statefulList?.submit(buildListState(state))
        }
    }

    private fun buildListState(state: ExportRecordsUiState): StatefulListLayout.State {
        val contentState = when {
            state.loading && !state.hasLoaded -> StatefulListLayout.ContentState.LOADING
            !state.error.isNullOrBlank() && state.records.isEmpty() ->
                StatefulListLayout.ContentState.ERROR
            state.hasLoaded && state.empty -> StatefulListLayout.ContentState.EMPTY
            else -> StatefulListLayout.ContentState.CONTENT
        }
        val showNoMore = state.hasLoaded && !state.hasMore && state.records.isNotEmpty() && !state.loadingMore
        return StatefulListLayout.State(
            contentState = contentState,
            loadingMore = state.loadingMore,
            noMore = showNoMore,
            errorText = state.error
        )
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
