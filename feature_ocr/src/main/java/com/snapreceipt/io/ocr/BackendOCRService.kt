package com.snapreceipt.io.ocr

import com.snapreceipt.io.ocr.model.OCRResult
import com.skybound.space.core.network.NetworkError
import com.skybound.space.core.network.NetworkResult
import com.snapreceipt.io.data.network.datasource.FileRemoteDataSource
import com.snapreceipt.io.data.network.datasource.ReceiptRemoteDataSource
import com.snapreceipt.io.data.network.datasource.UploadRemoteDataSource
import com.snapreceipt.io.data.network.model.ReceiptScanResultDto
import java.io.File

class BackendOCRService(
    private val fileRemoteDataSource: FileRemoteDataSource,
    private val uploadRemoteDataSource: UploadRemoteDataSource,
    private val receiptRemoteDataSource: ReceiptRemoteDataSource
) : OCRService {

    override suspend fun recognizeImage(imagePath: String): OCRResult? {
        val imageUrl = when (val urlResult = resolveImageUrl(imagePath)) {
            is NetworkResult.Success -> urlResult.data
            is NetworkResult.Failure -> return mapFailure(urlResult.error)
        }
        return when (val result = receiptRemoteDataSource.scan(imageUrl)) {
            is NetworkResult.Success -> OCRResult(
                code = 200,
                msg = "OK",
                data = toMap(result.data, imageUrl)
            )
            is NetworkResult.Failure -> mapFailure(result.error)
        }
    }

    private suspend fun resolveImageUrl(imagePath: String): NetworkResult<String> {
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return NetworkResult.Success(imagePath)
        }

        val file = File(imagePath)
        if (!file.exists()) {
            return NetworkResult.Failure(
                NetworkError.Unexpected("File not found: ${file.path}")
            )
        }

        val uploadInfo = when (val urlResult = fileRemoteDataSource.requestUploadUrl(file.name)) {
            is NetworkResult.Success -> urlResult.data
            is NetworkResult.Failure -> return NetworkResult.Failure(urlResult.error)
        }

        val contentType = guessContentType(file)
        val uploadResult = uploadRemoteDataSource.uploadFile(uploadInfo.uploadUrl, file, contentType)
        if (uploadResult is NetworkResult.Failure) {
            return NetworkResult.Failure(uploadResult.error)
        }

        return NetworkResult.Success(uploadInfo.publicUrl)
    }

    private fun guessContentType(file: File): String {
        return when (file.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }

    private fun toMap(result: ReceiptScanResultDto, imageUrl: String): Map<String, Any?> {
        return mapOf(
            "merchant" to result.merchant,
            "address" to result.address,
            "receiptDate" to result.receiptDate,
            "receiptTime" to result.receiptTime,
            "totalAmount" to result.totalAmount,
            "tipAmount" to result.tipAmount,
            "paymentCardNo" to result.paymentCardNo,
            "consumer" to result.consumer,
            "remark" to result.remark,
            "receiptUrl" to (result.receiptUrl ?: imageUrl)
        )
    }

    private fun mapFailure(error: NetworkError): OCRResult {
        return when (error) {
            is NetworkError.Http -> OCRResult(code = error.code, msg = error.message, data = null)
            is NetworkError.Network -> OCRResult(code = -1, msg = error.message, data = null)
            is NetworkError.Serialization -> OCRResult(code = -2, msg = error.message, data = null)
            is NetworkError.Unexpected -> OCRResult(code = -3, msg = error.message, data = null)
        }
    }
}

