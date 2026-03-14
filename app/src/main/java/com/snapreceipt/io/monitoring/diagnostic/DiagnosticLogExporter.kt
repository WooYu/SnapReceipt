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

@Singleton
class DiagnosticLogExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun createShareIntent(logFiles: List<File>): Result<Intent> = runCatching {
        require(logFiles.isNotEmpty()) { "No diagnostic logs available" }

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
                output.putNextEntry(ZipEntry(file.name))
                BufferedInputStream(FileInputStream(file)).use { input ->
                    input.copyTo(output)
                }
                output.closeEntry()
            }
        }
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
        private val EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
