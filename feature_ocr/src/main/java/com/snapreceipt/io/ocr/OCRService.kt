package com.snapreceipt.io.ocr

import com.snapreceipt.io.ocr.model.OCRResult

/**
 * OCR service interface for feature module (business logic).
 */
interface OCRService {
    suspend fun recognizeImage(imagePath: String): OCRResult?
}
