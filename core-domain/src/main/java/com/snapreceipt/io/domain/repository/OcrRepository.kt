package com.snapreceipt.io.domain.repository

import com.snapreceipt.io.domain.model.OcrResultEntity

interface OcrRepository {
    suspend fun recognizeImage(imagePath: String): OcrResultEntity
}
