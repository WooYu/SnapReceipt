package com.snapreceipt.io.ocr.repository

import com.snapreceipt.io.domain.model.OcrResultEntity
import com.snapreceipt.io.domain.repository.OcrRepository
import com.snapreceipt.io.ocr.OCRService
import com.snapreceipt.io.ocr.model.OCRResult
import javax.inject.Inject

class OcrRepositoryImpl @Inject constructor(
    private val ocrService: OCRService
) : OcrRepository {
    override suspend fun recognizeImage(imagePath: String): OcrResultEntity {
        val result = ocrService.recognizeImage(imagePath)
        return mapResult(result)
    }

    private fun mapResult(result: OCRResult?): OcrResultEntity {
        if (result == null) {
            throw IllegalStateException("OCR failed")
        }
        if (result.code != 0 && result.code != 200) {
            throw IllegalStateException(result.msg ?: "OCR failed")
        }
        val data = result.data
        val text = data?.get("text") as? String ?: ""
        val merchant = data?.get("merchant") as? String
            ?: data?.get("merchantName") as? String
        val amount = (data?.get("totalAmount") as? Number)?.toDouble()
            ?: (data?.get("amount") as? Number)?.toDouble()
            ?: (data?.get("totalAmount") as? String)?.toDoubleOrNull()
            ?: (data?.get("amount") as? String)?.toDoubleOrNull()
        return OcrResultEntity(
            text = text,
            merchant = merchant,
            amount = amount
        )
    }
}
