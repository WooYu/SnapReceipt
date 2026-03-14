package com.snapreceipt.io.monitoring.diagnostic

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.snapreceipt.io.monitoring.MonitoringConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 诊断日志导出器。
 *
 * 负责把多份日志打包成一个 ZIP，并生成带授权 URI 的分享 Intent，
 * 让用户可以直接通过邮箱、IM 等方式把日志发给排查人员。
 */
@Singleton
class DiagnosticLogExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun createShareIntent(logFiles: List<File>): Result<Intent> = runCatching {
        require(logFiles.isNotEmpty()) { "No diagnostic logs available" }

        // 导出目录位于缓存区，每次导出前先清理旧文件，避免把历史 ZIP 一直堆积在 cache 中。
        val exportDir = File(context.cacheDir, MonitoringConfig.DiagnosticLogs.exportDir).apply {
            mkdirs()
            listFiles()?.forEach { it.delete() }
        }
        val zipFile = File(
            exportDir,
            "${MonitoringConfig.DiagnosticLogs.exportZipPrefix}${EXPORT_TIME_FORMATTER.format(OffsetDateTime.now())}${MonitoringConfig.DiagnosticLogs.exportZipSuffix}"
        )
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { output ->
            logFiles.forEach { file ->
                // ZIP 中保留原始日志文件名，便于解压后直接判断当前日志和轮转日志。
                output.putNextEntry(ZipEntry(file.name))
                BufferedInputStream(FileInputStream(file)).use { input ->
                    input.copyTo(output)
                }
                output.closeEntry()
            }
        }
        // 通过 FileProvider 暴露只读 URI，避免把真实文件路径直接暴露给外部应用。
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )
        Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(zipFile.name, uri)
        }
    }

    private companion object {
        // 导出 ZIP 文件名包含时间戳，方便一次会话内多次导出时区分先后。
        private val EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
