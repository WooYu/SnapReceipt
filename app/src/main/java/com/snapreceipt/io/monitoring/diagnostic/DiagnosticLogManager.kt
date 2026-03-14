package com.snapreceipt.io.monitoring.diagnostic

import android.content.Context
import android.content.Intent
import com.snapreceipt.io.monitoring.MonitoringConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class DiagnosticLogManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exporter: DiagnosticLogExporter
) {

    @Volatile
    private var enabled: Boolean = false

    private val fileDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val fileScope = CoroutineScope(SupervisorJob() + fileDispatcher)

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun isEnabled(): Boolean = enabled

    fun append(level: String, tag: String, message: String, throwable: Throwable?) {
        if (!enabled) return
        val entry = buildEntry(level = level, tag = tag, message = message, throwable = throwable)
        fileScope.launch {
            runCatching {
                val currentLogFile = ensureCurrentLogFile()
                rotateIfNeeded(currentLogFile, entry.toByteArray(Charsets.UTF_8).size.toLong())
                currentLogFile.appendText(entry, Charsets.UTF_8)
                currentLogFile.parentFile?.let(::trimRotatedLogs)
            }
        }
    }

    suspend fun createShareIntent(): Result<Intent> = withContext(fileDispatcher) {
        exporter.createShareIntent(collectExportFiles())
    }

    private fun buildEntry(level: String, tag: String, message: String, throwable: Throwable?): String {
        return buildString {
            append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()))
            append(' ')
            append(level)
            append('/')
            append(tag)
            append(' ')
            appendLine(message)
            throwable?.let {
                appendLine(it.stackTraceToString().trimEnd())
            }
        }
    }

    private fun ensureCurrentLogFile(): File {
        val logDir = File(context.filesDir, MonitoringConfig.DiagnosticLogs.logsDir).apply { mkdirs() }
        return File(logDir, MonitoringConfig.DiagnosticLogs.currentFileName).apply {
            if (!exists()) {
                parentFile?.mkdirs()
                createNewFile()
            }
        }
    }

    private fun rotateIfNeeded(currentLogFile: File, incomingBytes: Long) {
        if (!currentLogFile.exists()) return
        if (currentLogFile.length() + incomingBytes <= MonitoringConfig.DiagnosticLogs.maxFileBytes) return

        val rotatedFile = File(
            currentLogFile.parentFile,
            "${MonitoringConfig.DiagnosticLogs.rotatedFilePrefix}${ROTATION_TIME_FORMATTER.format(OffsetDateTime.now())}${MonitoringConfig.DiagnosticLogs.rotatedFileSuffix}"
        )
        if (!currentLogFile.renameTo(rotatedFile)) {
            currentLogFile.copyTo(rotatedFile, overwrite = true)
            currentLogFile.delete()
        }
        currentLogFile.createNewFile()
    }

    private fun trimRotatedLogs(logDir: File) {
        logDir.listFiles()
            ?.filter { file ->
                file.name.startsWith(MonitoringConfig.DiagnosticLogs.rotatedFilePrefix) &&
                    file.name.endsWith(MonitoringConfig.DiagnosticLogs.rotatedFileSuffix) &&
                    file.name != MonitoringConfig.DiagnosticLogs.currentFileName
            }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MonitoringConfig.DiagnosticLogs.maxRotatedFiles)
            ?.forEach { it.delete() }
    }

    private fun collectExportFiles(): List<File> {
        val logDir = File(context.filesDir, MonitoringConfig.DiagnosticLogs.logsDir)
        if (!logDir.exists()) return emptyList()
        val currentFile = File(logDir, MonitoringConfig.DiagnosticLogs.currentFileName)
            .takeIf { it.exists() }
        val rotatedFiles = logDir.listFiles()
            ?.filter { file ->
                file.isFile &&
                    file.name.startsWith(MonitoringConfig.DiagnosticLogs.rotatedFilePrefix) &&
                    file.name.endsWith(MonitoringConfig.DiagnosticLogs.rotatedFileSuffix) &&
                    file.name != MonitoringConfig.DiagnosticLogs.currentFileName
            }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        return listOfNotNull(currentFile) + rotatedFiles
    }

    private companion object {
        private val ROTATION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
