package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.model.ReceiptScanResultEntity
import com.snapreceipt.io.domain.repository.FileRepository
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import java.io.File
import javax.inject.Inject

class UploadAndScanReceiptUseCase @Inject constructor(
    private val fileRepository: FileRepository,
    private val receiptRepository: ReceiptRemoteRepository
) {
    suspend operator fun invoke(filePath: String): Result<ReceiptScanResultEntity> =
        runCatching {
            val file = File(filePath)
            val uploadInfo = fileRepository.requestUploadUrl(file.name)
            fileRepository.uploadFile(uploadInfo.uploadUrl, file.absolutePath, "image/jpeg")
            val scanResult = receiptRepository.scan(uploadInfo.publicUrl)
            if (scanResult.receiptUrl.isNullOrBlank()) {
                scanResult.copy(receiptUrl = uploadInfo.publicUrl)
            } else {
                scanResult
            }
        }
}
