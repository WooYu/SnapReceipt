package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import com.snapreceipt.io.domain.usecase.file.RequestUploadUrlUseCase
import com.snapreceipt.io.domain.usecase.file.UploadFileUseCase
import java.io.File
import javax.inject.Inject

/**
 * 上传本地图片并触发扫码识别的组合用例。
 */
class UploadAndScanReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRemoteRepository,
    private val requestUploadUrlUseCase: RequestUploadUrlUseCase,
    private val uploadFileUseCase: UploadFileUseCase
) {
    /**
     * 当前执行阶段，用于通知 UI 展示进度。
     */
    enum class Stage {
        REQUESTING_UPLOAD_URL,
        UPLOADING,
        SCANNING
    }

    /**
     * 先申请上传地址，再上传文件，最后调用扫码解析接口。
     *
     * @param filePath 本地图片路径
     * @param onProgress 进度回调（阶段变更时调用）
     */
    suspend operator fun invoke(
        filePath: String,
        onProgress: (Stage) -> Unit = {}
    ): Result<ReceiptEntity> =
        runCatching {
            val file = File(filePath)
            if (!file.exists()) error("File not found: ${file.absolutePath}")

            // 1) 获取直传地址
            onProgress(Stage.REQUESTING_UPLOAD_URL)
            val uploadInfo = requestUploadUrlUseCase(file.name).getOrThrow()

            // 2) 上传文件到对象存储
            onProgress(Stage.UPLOADING)
            val contentType = guessContentType(file)
            uploadFileUseCase(uploadInfo.uploadUrl, filePath, contentType).getOrThrow()

            // 3) 调用识别接口
            onProgress(Stage.SCANNING)
            val scanResult = receiptRepository.scan(uploadInfo.publicUrl)
            // 扫码结果可能缺少回填的图片地址，兜底使用上传后的公开地址
            val resolvedUrl = scanResult.receiptUrl?.takeIf { it.isNotBlank() } ?: uploadInfo.publicUrl
            scanResult.copy(receiptUrl = resolvedUrl)
        }

    private fun guessContentType(file: File): String {
        // 仅根据扩展名推断，避免依赖 ContentResolver
        return when (file.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }
}
