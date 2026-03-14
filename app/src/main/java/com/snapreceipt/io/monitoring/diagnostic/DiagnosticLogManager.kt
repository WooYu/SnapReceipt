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

/**
 * 本地诊断日志管理器。
 *
 * 负责把应用运行期间的重要日志异步写入私有目录，并在文件过大时自动轮转。
 * 这条链路主要服务于“线上问题需要用户导出日志排查”的场景。
 */
@Singleton
class DiagnosticLogManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exporter: DiagnosticLogExporter
) {

    // 诊断日志属于高频 IO，单独开关便于线上按需启用。
    @Volatile
    private var enabled: Boolean = false

    // 文件写入必须串行化，避免并发 append 导致日志交错或轮转时机错乱。
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
                // 先判断轮转再写入，确保当前文件始终是“最新可继续追加”的那一个。
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
        // 使用 ISO_OFFSET_DATE_TIME，便于跨时区排查时保留精确偏移信息。
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
        // 当前日志文件固定命名，导出时可以稳定地拿到“正在写入”的文件。
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
        // renameTo 更快；如果跨文件系统或系统限制导致失败，再降级为复制+删除。
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
            // 始终保留最新若干份历史日志，老文件自动回收，避免占满私有存储。
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
        // 轮转文件名按时间排序，便于用户和开发同学快速定位生成顺序。
        private val ROTATION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
