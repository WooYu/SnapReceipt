package com.snapreceipt.io.domain.usecase.receipt

import android.util.Base64
import com.snapreceipt.io.domain.model.ReceiptScanResultEntity
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import java.io.File
import java.net.URLConnection
import javax.inject.Inject

class UploadAndScanReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRemoteRepository
) {
    suspend operator fun invoke(filePath: String): Result<ReceiptScanResultEntity> =
        runCatching {
            val file = File(filePath)
            if (!file.exists()) error("File not found: ${file.absolutePath}")
            val dataUri = encodeToDataUri(file)
            val scanResult = receiptRepository.scan(dataUri)
            if (scanResult.receiptUrl.isNullOrBlank()) {
                scanResult.copy(receiptUrl = dataUri)
            } else {
                scanResult
            }
        }

    private fun encodeToDataUri(file: File): String {
        val bytes = file.readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val mime = URLConnection.guessContentTypeFromName(file.name)?.takeIf { it.isNotBlank() }
            ?: "image/jpeg"
        return "data:$mime;base64,$base64"
    }
}
