package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.model.ReceiptScanResultEntity
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import com.snapreceipt.io.domain.usecase.file.RequestUploadUrlUseCase
import com.snapreceipt.io.domain.usecase.file.UploadFileUseCase
import java.io.File
import javax.inject.Inject

class UploadAndScanReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRemoteRepository,
    private val requestUploadUrlUseCase: RequestUploadUrlUseCase,
    private val uploadFileUseCase: UploadFileUseCase
) {
    enum class Stage {
        REQUESTING_UPLOAD_URL,
        UPLOADING,
        SCANNING
    }

    suspend operator fun invoke(
        filePath: String,
        onProgress: (Stage) -> Unit = {}
    ): Result<ReceiptScanResultEntity> =
        runCatching {
            val file = File(filePath)
            if (!file.exists()) error("File not found: ${file.absolutePath}")

            onProgress(Stage.REQUESTING_UPLOAD_URL)
            val uploadInfo = requestUploadUrlUseCase(file.name).getOrThrow()

            onProgress(Stage.UPLOADING)
            val contentType = guessContentType(file)
            uploadFileUseCase(uploadInfo.uploadUrl, filePath, contentType).getOrThrow()

            onProgress(Stage.SCANNING)
            val scanResult = receiptRepository.scan(uploadInfo.publicUrl)
            if (scanResult.receiptUrl.isNullOrBlank()) {
                scanResult.copy(receiptUrl = uploadInfo.publicUrl)
            } else {
                scanResult
            }
        }

    private fun guessContentType(file: File): String {
        return when (file.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }
}
